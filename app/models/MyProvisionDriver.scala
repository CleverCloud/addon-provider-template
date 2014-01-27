package models

import utils.ProvisionDriverModule

trait MyProvisionDriver extends ProvisionDriverModule {
	def driver = new ProvisionDriver {
		def provision(pd: ProvisionData): Either[String,AddonData] = Right(
			AddonData(
				"myaddon_dummy",
				pd.heroku_id,
				pd.plan,
				pd.region,
				pd.callback_url,
				pd.logplex_token,
				Map(
					"MY_ADDON_USER" -> "someuser",
				  	"MY_ADDON_PASSWORD" -> "somepassword"
				)
			)
		)
		def deprovision(ad: AddonData): Either[String,AddonData] = Left("Not implemented yet")
		def changePlan(ad: AddonData, plan: String): Either[String, AddonData] = Left("Not implemented yet")
	}
}

