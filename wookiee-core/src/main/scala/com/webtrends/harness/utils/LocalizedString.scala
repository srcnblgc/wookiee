package com.webtrends.harness.utils

import java.text.MessageFormat
import java.util.{ResourceBundle, Locale}

/** Messages externalization
  *
  * == Overview ==
  * You would use it like so:
  *
  * {{{
  * Localized(user) { implicit lang =>
  *   val error = LocalizedString("error")
  * }
  * }}}
  *
  * Messages are stored in `messages_XXX.properties` files in UTF-8 encoding in resources.
  * The lookup will fallback to default file `messages.properties` if the string is not found in
  * the language-specific file.
  *
  * Messages are formatted with `java.text.MessageFormat`.
  */
trait LocalizedString {
  /** get the message w/o formatting */
  def raw(msg: String)(implicit locale: Locale=Locale.getDefault, context:String="messages"): String = {
    val bundle = ResourceBundle.getBundle(context, locale, UTF8BundleControl)
    bundle.getString(msg)
  }

  def apply(msg: String, args: Any*)(locale: Locale=Locale.getDefault, context:String="messages"): String = {
    new MessageFormat(raw(msg)(locale, context), locale).format(args.map(_.asInstanceOf[java.lang.Object]).toArray)
  }
}


object LocalizedString extends LocalizedString


// @see https://gist.github.com/alaz/1388917
// @see http://stackoverflow.com/questions/4659929/how-to-use-utf-8-in-resource-properties-with-resourcebundle
private[utils] object UTF8BundleControl extends ResourceBundle.Control {

  val Format = "properties.utf8"

  override def getFormats(baseName: String): java.util.List[String] = {
    import collection.JavaConverters._

    Seq(Format).asJava
  }

  override def getFallbackLocale(baseName: String, locale: Locale) =
    if (locale == Locale.getDefault) null
    else Locale.getDefault

  override def newBundle(baseName: String, locale: Locale, fmt: String, loader: ClassLoader, reload: Boolean): ResourceBundle = {
    import java.util.PropertyResourceBundle
    import java.io.InputStreamReader

    // The below is an approximate copy of the default Java implementation
    def resourceName = toResourceName(toBundleName(baseName, locale), "properties")

    def stream =
      if (reload) {
        for {url <- Option(loader getResource resourceName)
             connection <- Option(url.openConnection)}
          yield {
            connection.setUseCaches(false)
            connection.getInputStream
          }
      } else
        Option(loader getResourceAsStream resourceName)

    (for {format <- Option(fmt) if format == Format
          is <- stream}
      yield new PropertyResourceBundle(new InputStreamReader(is, "UTF-8"))).orNull
  }
}