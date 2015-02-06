package models

/**
 * These are the classes that are needed to run a simple addon API.
 * As we want to integrate seamlessly with the heroku addon API to
 * permit vendors to get faster to work, expect to receive jsons with
 * Heroku-flagged fields.
 **/

/**
 * This is sent by the user (that is, Clever Cloud's server) to
 * provision an addon.
 **/
case class ProvisionData(
	heroku_id: String, // Id of the application/addon from Clever Cloud's point of view
  	plan: String, // Wanted plan's name.
  	region: String, // Region. For now, only EU is valid.
	callback_url: String, // URL to request user's data to the Clever Cloud's API
	logplex_token: String, // Token you need to send logs to our logging system.
	options: Map[String,String] // These are only here to add parameters to creation. No need to store'em
)

/**
 * This is used for respond to provision requests.
 * If we respond an error, the id field will be "error"
 **/
case class ProvisionResponse(
	id: String, // The addon id from this API POV.
	message: String, // A creation message
	config: Map[String,String] = Map.empty // config data (variables to be injected into applications to contact the service you provide
)

/**
 * This is sent by Clever's API to update an addon's plan.
 */
case class PlanChangeData(
	heroku_id: String, // The addon's id from Clever's POV.
	plan: String // new plan name.
)

case class AddonConfig(
	host: String,
	user: String,
	password: String
) {            
   def toMap: Map[String, String] = Map(
      "MY_ADDON_URL" -> this.host,
      "MY_ADDON_LOGIN" -> this.user,
      "MY_ADDON_PASSWORD" -> this.password
   )
}

/**
 * This is an internal representation of an addon.
 * It's kind of a mix between ProvisionData and ProvisionRespons
 */
case class AddonData(
	id: String, // Id from the provider's POV.
	appId: String, // Id from the Clever's POV.
  	plan: String, // Plan name.
  	region: String, // Region (so, for now, EU).
	callback_url: String, // URL to request user's data to the Clever Cloud's API
	logplex_token: String, // Token you need to send logs to our logging system.
	config: AddonConfig // Config that will be stored as environment variables.
)

/**
 * Class to map an SSO request's params.
 */
case class SSOData(
	id: String, // Id from the provider's POV.
	timestamp: Long, // timestamp of the token creation. In millis.
	token: String, // token used to sign the request.
	navData: String, // Some data to put in the session.
	email: String // email of the requesting user.
)


object JsonFormats {
	import play.api.libs.json._
	import play.api.libs.functional.syntax._
	import play.api.data.validation.ValidationError

	implicit val addonConfigFormat = Json.format[AddonConfig]
	implicit val provisionDataFormat = Json.format[ProvisionData]
	implicit val provisionResponseFormat = Json.format[ProvisionResponse]
	implicit val planChangeFormat = Json.format[PlanChangeData]
}
