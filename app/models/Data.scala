package models

import anorm._
import play.api.db.DB
import play.api.Play.current
import anorm.SqlParser._
import java.util.UUID
import utils.PersistenceModule

trait ProvisionPersistence extends PersistenceModule {
	import scala.language.postfixOps

	def persistence = new Persistence {
		def persist(p: AddonData): Either[String,AddonData] = DB.withConnection { implicit c =>
		{
			SQL("""
				insert into provision(id, app_id, plan, region, callback_url, logplex_token)
				values ({id}, {app_id}, {plan}, {region}, {callback_url}, {logplex_token})
				"""
			).on(
				"id" -> p.id,
				"app_id" -> p.appId,
				"plan" -> p.plan,
				"region" -> p.region,
				"callback_url" -> p.callback_url,
				"logplex_token" -> p.logplex_token
			).executeUpdate()

			p.config.foreach {
				case (k,v) =>
					SQL("""
						insert into provision_config(provision_id, key, value)
						values ({id},{key},{value})
						"""
					).on(
						"id" -> p.id,
						"key" -> k,
						"value" -> v
					).executeUpdate()
			}
			Right(p)
		}}

		def get(id: String): Option[AddonData] = DB.withConnection { implicit c =>
			val row: Option[String~String~String~String~String~String] = SQL("SELECT * FROM provision WHERE provision.id = '{id}'")
				.on("id" -> id)
				.as(
					str("id") ~
					str("app_id") ~
					str("plan") ~
					str("region") ~
					str("callback_url") ~
					str("logplex_token")*
				).headOption

			val config: Map[String,String] = SQL(
				"""
					SELECT key,value
					FROM provision_config
					WHERE provision_id = {id}
				"""
			).on("id" -> id).as(str("key")~str("value")*).map {
				case key~value => key -> value
			}.toMap

			row.map {
				case id~appId~plan~region~callbackUrl~lptoken => AddonData(id,appId,plan,region,callbackUrl,lptoken,config)
			}
		}

		def delete(id: String): Either[String,AddonData] = DB.withConnection { implicit c =>
			get(id).map(data => {
				SQL("DELETE FROM provision WHERE id = '{id}'")
					.on("id" -> id)
					.executeUpdate
				Right(data)
			}).getOrElse(Left("This addon does not exist"))
		}

		def changePlan(id: String, plan: String): Either[String,AddonData] = DB.withConnection { implicit c =>
			get(id).map(data => {
				if(data.plan == plan)
					Left("I won't change the plan to set the same one")
				else {
					SQL("UPDATE provision SET plan = '{plan}' WHERE id = '{id}'")
						.on("plan" -> plan, "id" -> id)
						.executeUpdate
					Right(data)
				}
			}).getOrElse(Left("The Addon does not exist"))
		}


	}
}

object JsonFormats {
	import play.api.libs.json._
	import play.api.libs.functional.syntax._
	import play.api.data.validation.ValidationError

	implicit val provisionDataFormat = Json.format[ProvisionData]
	implicit val provisionResponseFormat = Json.format[ProvisionResponse]
}
