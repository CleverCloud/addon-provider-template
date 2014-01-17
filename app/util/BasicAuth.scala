package utils

import play.api.mvc._, Results._
import play.api.Play
import play.api.Play.current

trait BasicAuth {


	private def decodeBasicAuth(request: Request[Any]): Option[(String,String)] =
		request.headers.get("Authorization").map(auth => {
			val baStr = auth.replaceFirst("Basic ", "")
			val Array(user, pass) = new String(new sun.misc.BASE64Decoder().decodeBuffer(baStr), "UTF-8").split(":")
			(user, pass)
		})

	def Authenticated[A](p: BodyParser[A])(f: Request[A] => Result): Action[A] = {
		Action(p) { request =>
			val result = for {
				(user,pass) <- decodeBasicAuth(request)
				authUser <- Play.configuration.getString("api.auth.user")
				authPassword <- Play.configuration.getString("api.auth.password")
			} yield if (user == authUser && pass == authPassword) f(request) else Unauthorized
			result getOrElse Unauthorized
		}
	}

	import play.api.mvc.BodyParsers._
	def Authenticated(f: Request[AnyContent] => Result): Action[AnyContent]  = {
		Authenticated(parse.anyContent)(f)
	}
}


