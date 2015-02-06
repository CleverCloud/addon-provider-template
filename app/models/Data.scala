package models

import anorm._
import play.api.db.DB
import play.api.Play.current
import anorm.SqlParser._
import java.util.UUID
import utils.PersistenceModule

trait ProvisionPersistence extends PersistenceModule {
	import scala.language.postfixOps
	import java.util.Date

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

			p.config.toMap.foreach {
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

		def filterErrors(ads: List[Either[String, AddonData]]): List[String] = ads.flatMap(_.fold(s => Some(s),_ => None))

		def filterAddons(ads: List[Either[String,AddonData]]): List[AddonData] = ads.flatMap(_.fold(_ => None, a => Some(a)))

		private val addonDataParser =
			str("id") ~
			str("app_id") ~
			str("plan") ~
			str("region") ~
			str("callback_url") ~
			str("logplex_token") ~
			long("cluster_id") ~
			date("creation_date") ~
			get[Option[Date]]("deletion_date") ~
			str("status")*

		private def getConfig(id: String): Either[String,AddonConfig] = DB.withConnection { implicit c =>
			val themap = SQL(
				"""
					SELECT key,value
					FROM provision_config
					WHERE provision_id = {id}
				"""
			).on("id" -> id).as(str("key")~str("value")*).map {
				case key~value => key -> value
			}.toMap
			for {
				host <- themap.get("MY_ADDON_HOST").map(Right(_)).getOrElse(Left("Error: no host config for " + id)).right
				user <- themap.get("MY_ADDON_LOGIN").map(Right(_)).getOrElse(Left("Error: no user config for " + id)).right
				password <- themap.get("MY_ADDON_PASSWORD").map(Right(_)).getOrElse(Left("Error: no password config for " + id)).right
			} yield AddonConfig(host, user, password)
		}

		private def extractAddon(query: SimpleSql[Row])(implicit c: java.sql.Connection): List[Either[String,AddonData]] =
			query.as(addonDataParser).map {
				case id~appId~plan~region~callbackUrl~lptoken~clusterId~creationDate~deletionDate~status => {
					for {
						config <- getConfig(id).right
					} yield AddonData(
						id
						,appId
						,plan
						,region
						,callbackUrl
						,lptoken
						,config
					)
				}
			}

		def find(id: String): Option[AddonData] = DB.withConnection { implicit c =>
         filterAddons(extractAddon(
            SQL("SELECT * FROM provision WHERE id = {id}")
            .on("id" -> id)
         )).headOption
		}

		def delete(id: String): Either[String,AddonData] = DB.withConnection { implicit c =>
			find(id).map(data => {
				SQL("DELETE FROM provision WHERE id = {id}")
					.on("id" -> id)
					.executeUpdate
				Right(data)
			}).getOrElse(Left("This addon does not exist"))
		}

		def changePlan(id: String, plan: String): Either[String,AddonData] = DB.withConnection { implicit c =>
			find(id).map(data => {
				if(data.plan == plan)
					Left("I won't change the plan to set the same one")
				else {
					SQL("UPDATE provision SET plan = '{plan}' WHERE id = {id}")
						.on("plan" -> plan, "id" -> id)
						.executeUpdate
					Right(data)
				}
			}).getOrElse(Left("The Addon does not exist"))
		}

		def findByAppId(appId: String): Option[AddonData] = DB.withConnection { implicit c =>
			filterAddons(
				extractAddon(
					SQL("SELECT * FROM provision WHERE app_id = {appId}").on("appId" -> appId)
				)
			).headOption
		}
	}
}

