package utils

import play.api.libs.json._

trait Log {
  def logger: play.api.Logger

  def trace(msg: String) = logger.trace(msg)
  def trace(errors: Seq[(JsPath, Seq[play.api.data.validation.ValidationError])]) =
    logger.trace(Json.prettyPrint(JsError.toFlatJson(errors)))

  def debug(msg: String) = logger.debug(msg)
  def debug(errors: Seq[(JsPath, Seq[play.api.data.validation.ValidationError])]) =
    logger.debug(Json.prettyPrint(JsError.toFlatJson(errors)))

  def info(msg: String) = logger.info(msg)
  def warn(msg: String) = logger.warn(msg)
  def error(msg: String) = logger.error(msg)
}
