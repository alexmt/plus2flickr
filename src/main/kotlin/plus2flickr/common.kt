package plus2flickr

import plus2flickr.domain.AccountType
import plus2flickr.thirdparty.CloudService
import plus2flickr.thirdparty.UrlResolver
import plus2flickr.thirdparty.ImageSize

class CloudServiceContainer {
  val accountTypeToService = hashMapOf<AccountType, CloudService>()

  fun register(accountType: AccountType, service: CloudService) {
    accountTypeToService.put(accountType, service)
  }

  fun get(accountType: AccountType): CloudService {
    val service = accountTypeToService[accountType]
    if (service == null) {
      throw IllegalArgumentException("Service '$accountType' is not supported.")
    }
    return service
  }
}

class ServiceUrlResolver(val accountType: AccountType) : UrlResolver {
  override fun getPhotoRedirectUrl(id: String, size: ImageSize) = "/services/user/photo/redirect/$accountType/$id/$size"
}