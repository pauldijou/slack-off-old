package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import models._
import services._

object Bitbuckets extends Controller with utils.Config with utils.Log {
  lazy val logger = Logger("hooks.bitbucket")

  def handlePostHook = Action { implicit request =>
    debugStart("Bitbuckets.handlePostHook")
    debug(request.body.toString)

    val body: JsValue = request.body match {
      case json: AnyContentAsJson => json.asJson.getOrElse(JsUndefined("Request body is not valid JSON."))
      case form: AnyContentAsFormUrlEncoded => {
        (for {
          mapForm <- form.asFormUrlEncoded
          payload <- mapForm.get("payload")
          payloadContent <- payload.headOption
        } yield {
          Json.parse(payloadContent)
        }) getOrElse(JsUndefined("Request body payload is not valid JSON."))
      }
      case _ => JsUndefined("Body type not supported")
    }

    debug(Json.prettyPrint(body))

    body.validate[BitbucketPostHook].fold(
      errors => debug(errors),
      hook => {
        debug(hook.toString)
        val username = request.getQueryString("username") orElse Some("Bitbucket")
        val channel = request.getQueryString("channel").map("#" + _)
        val iconUrl = request.getQueryString("iconUrl") orElse
          Some("https://slack.global.ssl.fastly.net/10800/img/services/bitbucket_32.png")


        val projectUrl = s"${hook.canon_url}${hook.repository.absolute_url}"
        val commits = hook.commits
        val commitsPlural = if (commits.size > 1) { "s" } else { "" }
        val totalFiles = commits.foldLeft(0) { (acc, c) => acc + c.files.size }
        val totalFilesPlural = if (totalFiles > 1) { "s" } else { "" }
        val message = s"[<${projectUrl}|${hook.repository.name}>] ${commits.size} commit${commitsPlural} impacting ${totalFiles} file${totalFilesPlural}"
        var commitsBuffer = scala.collection.mutable.ListBuffer[IncomingWebHookAttachment]()

        commits foreach { commit =>
          val commitUrl = commit.browseUrl(projectUrl)
          val branchUrl = commit.browseBranchCommitsUrl(projectUrl)
          val authorUrl = commit.browseAuthorUrl(hook.canon_url)
          val filesPlural = if (commit.files.size > 1) { "s" } else { "" }
          val filesMsg = s"${commit.files.size} file${filesPlural} impacted"
          val msg = s"[<${commitUrl}|${commit.node}>] <${authorUrl}|${commit.author}> on <${branchUrl}|${commit.branch}>\n${filesMsg} at ${commit.timestamp}"
          commitsBuffer += IncomingWebHookAttachment(
            msg + "\n" + commit.message, None, None, None,
            List(
              IncomingWebHookAttachmentField(commit.message, msg)
            )
          )
        }

        val attachments = commitsBuffer.result
        val attachmentsOpt = if (attachments.isEmpty) { None } else { Some(attachments) }

        IncomingWebhooks.send(IncomingWebHook(message, username, channel, iconUrl, None, attachmentsOpt))
      }
    )

    debugEnd
    Ok
  }
}
