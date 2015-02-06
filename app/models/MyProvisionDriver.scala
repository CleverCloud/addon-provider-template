package models

import utils.ProvisionDriverModule

trait MyProvisionDriver extends ProvisionDriverModule {
	def driver = new ProvisionDriver {
		import org.apache.commons.lang3.RandomStringUtils
		def provision(pd: ProvisionData): Either[String,AddonData] = Right(
			AddonData(
				"myaddon_"+java.util.UUID.randomUUID().toString,
				pd.heroku_id,
				pd.plan,
				pd.region,
				pd.callback_url,
				pd.logplex_token,
				AddonConfig("myhost.com",RandomStringUtils.randomAlphanumeric(10),RandomStringUtils.randomAlphanumeric(20))
			)
		)
		def deprovision(ad: AddonData): Either[String,AddonData] = Left("Not implemented yet")
		def changePlan(ad: AddonData, plan: String): Either[String, AddonData] = Left("Not implemented yet")
	}
}

