package models

case class ProvisionData(
	heroku_id: String,
  	plan: String,
  	region: String,
	callback_url: String,
	logplex_token: String,
	options: Map[String,String] // These are only here to add parameters to creation. No need to store'em
)

case class ProvisionResponse(
	id: String,
	message: String,
	config: Map[String,String] = Map.empty
)

case class PlanChangeData(
	heroku_id: String,
	plan: String
)

case class AddonData(
	id: String,
	appId: String,
  	plan: String,
  	region: String,
	callback_url: String,
	logplex_token: String,
	config: Map[String,String]
)


object JsonFormats {
	import play.api.libs.json._
	import play.api.libs.functional.syntax._
	import play.api.data.validation.ValidationError

	implicit val provisionDataFormat = Json.format[ProvisionData]
	implicit val addonDataFormat = Json.format[AddonData]
	implicit val provisionResponseFormat = Json.format[ProvisionResponse]
	implicit val planChangeFormat = Json.format[PlanChangeData]
}
