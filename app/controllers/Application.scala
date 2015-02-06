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
				Ok(Json.toJson(ProvisionResponse(addonData.right.get.id,"Created",addonData.right.get.config.toMap)))
		}).getOrElse(BadRequest)
	}

	def deprovision(id: String) = Authenticated { request =>
		persistence.find(id).map(deprovData => {
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
				val addonData = persistence.find(id)
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
							Ok(Json.toJson(ProvisionResponse(planChanged.right.get.id,"Plan changed",planChanged.right.get.config.toMap)))
					}
				}).getOrElse(NotFound(Json.toJson(ProvisionResponse("error","This addon does not exist"))))
			}).getOrElse(BadRequest(Json.toJson(ProvisionResponse("error","Bad data sent"))))
	}

}
