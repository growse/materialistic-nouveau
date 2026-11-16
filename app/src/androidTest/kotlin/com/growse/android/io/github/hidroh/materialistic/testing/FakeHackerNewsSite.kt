package com.growse.android.io.github.hidroh.materialistic.testing

/**
 * Serves news.ycombinator.com's form endpoints from [FakeBackend], answering the way the site does:
 * an accepted form redirects (302), a rejected one re-renders its page (200).
 */
object FakeHackerNewsSite {
  private const val SITE = "https://news.ycombinator.com"

  fun loginSucceeds() = redirect("/login", to = "/news")

  /** Re-renders the login page with [reason] where the site shows it. */
  fun loginFails(reason: String = "Bad login.") =
      FakeBackend.respond("$SITE/login", "<html><body>$reason</body></html>")

  fun voteSucceeds() = redirect("/vote", to = "/news")

  fun replySucceeds() = redirect("/comment", to = "/item?id=1")

  /** Turns the reply away as it does for stale credentials, which the app takes as a re-login. */
  fun replyRejected() = FakeBackend.respond("$SITE/comment", "<html><body></body></html>")

  /**
   * Accepts the login on the submit page and hands out [fnid], the form token the submission itself
   * must carry.
   */
  fun submitPageIssues(fnid: String) =
      FakeBackend.respond(
          "$SITE/submit",
          """<html><body><form><input type="hidden" name="fnid" value="$fnid"></form></body></html>""",
      )

  /** Accepts the submission, redirecting to the newest stories as the site does. */
  fun submissionAccepted() = redirect("/r", to = "/newest")

  /** Turns the submission away as a duplicate of the story [existingId]. */
  fun submissionIsDuplicateOf(existingId: Long) = redirect("/r", to = "/item?id=$existingId")

  private fun redirect(path: String, to: String) =
      FakeBackend.respond("$SITE$path", code = 302, headers = mapOf("Location" to to))
}
