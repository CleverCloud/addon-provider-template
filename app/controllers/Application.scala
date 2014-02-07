package controllers

import play.api._
import play.api.mvc._
import play.api.Play.current

/**
 * Your application need to extends the ProvisionPersistenceModule and
 * the ProvisionDriverModule traits. To extend them, best is to
 * "implement" them using other traits. You might only need to extends
 * the right traits and not change anything to this file.
 *
 */
object Application extends Controller with utils.BasicAuth with models.ProvisionPersistence with models.MyProvisionDriver {

	import models._,JsonFormats._
	import play.api.libs.json._
	import play.api.data._
	import play.api.data.Forms._
	import org.joda.time.DateTime

	val ssoDataForm = Form(
		mapping(
			"id" -> nonEmptyText,
			"timestamp" -> longNumber,
			"token" -> nonEmptyText,
			"nav-data" -> text,
			"email" -> nonEmptyText
		)(SSOData.apply)(SSOData.unapply)
	)

	def provision = Authenticated(parse.json) { request =>
		val provdata = request.body.asOpt[ProvisionData]
		provdata.map(pdata => {
			val addonData = for {
				provisioned <- driver.provision(pdata).right
				persisted <- persistence.persist(provisioned).right
			} yield persisted

			if(addonData.isLeft)
				InternalServerError(Json.toJson(ProvisionResponse("error",addonData.left.get)))
			else
				Ok(Json.toJson(ProvisionResponse(addonData.right.get.id,"Created",addonData.right.get.config)))
		}).getOrElse(BadRequest)
	}

	def deprovision(id: String) = Authenticated { request =>
		persistence.get(id).map(deprovData => {
				val addonData = for {
					deprovisioned <- driver.deprovision(deprovData).right
					deleted <- persistence.delete(deprovisioned.id).right
				} yield deleted
				if(addonData.isLeft)
					InternalServerError(Json.toJson(ProvisionResponse("error", addonData.left.get)))
				else
					Ok
			}).getOrElse(BadRequest(Json.toJson(ProvisionResponse("error","This addon does not exist"))))
	}

	def changePlan(id: String) = Authenticated(parse.json) { request =>
		val planChangeData = request.body.asOpt[PlanChangeData]
		planChangeData.map(pcd => {
				val addonData = persistence.get(id)
				addonData.map(ad => {
					if(ad.appId != pcd.heroku_id)
						BadRequest(Json.toJson(ProvisionResponse("error","You provided a bad heroku_id")))
					else {
						val planChanged = for {
							drivered <- driver.changePlan(ad, pcd.plan).right
							changed <- persistence.changePlan(drivered.id,pcd.plan).right
						} yield changed
						if(planChanged.isLeft)
							InternalServerError(Json.toJson(ProvisionResponse("error", planChanged.left.get)))
						else
							Ok(Json.toJson(ProvisionResponse(planChanged.right.get.id,"Plan changed",planChanged.right.get.config)))
					}
				}).getOrElse(NotFound(Json.toJson(ProvisionResponse("error","This addon does not exist"))))
			}).getOrElse(BadRequest(Json.toJson(ProvisionResponse("error","Bad data sent"))))
	}

	def dashboard = Action { implicit request =>
		request.session.get("id").flatMap(id =>
			persistence.get(id).map(data => Ok(views.html.dashboard(data)))
		).getOrElse(Forbidden(Json.toJson(ProvisionResponse("error", "You cannot have access to this addon"))))
	}

	def ssoEntryPoint = Action { implicit request =>
		import java.security.MessageDigest
		import org.apache.commons.codec.digest.DigestUtils
		ssoDataForm.bindFromRequest.fold(
			formWithErrors => {
				BadRequest
			},
			ssoData => {
				Play.configuration.getString("sso_salt")
					.map(salt => {
							val digest = DigestUtils.sha1Hex(ssoData.id + ":" + salt + ":" + ssoData.timestamp.toString)
							println("Calculated digest: " + digest)
							println("Given token: " + ssoData.token)
							val requestTime = new DateTime(ssoData.timestamp)
							if(ssoData.token == digest) {
								if(
									new DateTime().minusMinutes(5).isBefore(requestTime) &&
									new DateTime().isAfter(requestTime)) {
									if(persistence.get(ssoData.id).isEmpty)
										BadRequest(Json.toJson(ProvisionResponse("error","The addon does not exist")))
									else
										SeeOther(routes.Application.dashboard.url).withSession(
											"id" -> ssoData.id,
											"nav-data" -> ssoData.navData,
											"email" -> ssoData.email
										)
								} else {
									println("token is too old")
									Forbidden(Json.toJson(ProvisionResponse("error","The token is too old")))
								}
							} else {
								println("Token is not the right one")
								Forbidden(Json.toJson(ProvisionResponse("error", "You cannot have access to this addon")))
							}
						}).getOrElse(InternalServerError(Json.toJson(ProvisionResponse("error", "No sso salt in configuration"))))
			}
		)
	}

}
