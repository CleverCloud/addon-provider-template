package models

import utils.ProvisionDriverModule

trait MyProvisionDriver extends ProvisionDriverModule {
	def driver = new ProvisionDriver {
		def provision(pd: ProvisionData): Either[String,AddonData] = Left("Not implemented yet")
		def deprovision(ad: AddonData): Either[String,AddonData] = Left("Not implemented yet")
		def changePlan(ad: AddonData, plan: String): Either[String, AddonData] = Left("Not implemented yet")
	}
}

