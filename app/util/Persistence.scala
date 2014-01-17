package utils

import models.AddonData

trait PersistenceModule {
	def persistence: Persistence
	trait Persistence {
		def persist(data: AddonData): Either[String,AddonData] // Return the persisted AddonData
		def delete(id: String): Either[String, AddonData] // Return the deleted AddonData
		def get(id: String): Option[AddonData]
		def changePlan(id: String, plan: String): Either[String, AddonData]
	}
}

