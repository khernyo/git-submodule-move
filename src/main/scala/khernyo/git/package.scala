package khernyo

import java.nio.file.Path

package object git {

  implicit class RichPath(val f: Path) extends AnyVal {
    def /(s: String) = f.resolve(s)
    def /(p: Path) = f.resolve(p)
  }

}
