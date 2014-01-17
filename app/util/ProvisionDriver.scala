package utils

import models._

trait ProvisionDriverModule {
	def driver: ProvisionDriver

	trait ProvisionDriver {
		def provision(pd: ProvisionData): Either[String,AddonData]
		def deprovision(ad: AddonData): Either[String,AddonData]
		def changePlan(ad: AddonData, plan: String): Either[String, AddonData]
	}
}

