package khernyo.git

import language.postfixOps
import org.eclipse.jgit.util.FS
import org.eclipse.jgit.storage.file.{ FileRepositoryBuilder, FileBasedConfig }
import collection.JavaConverters._
import java.nio.file.{ FileSystems, Files, Path }
import java.io.PrintWriter
import org.eclipse.jgit.api.{ RmCommand, AddCommand }

object GitSubmoduleMove {
  def main(args: Array[String]) {
    if (args.length != 2) {
      println(s"Usage git-submodule-move <submodule-from> <submodule-to>")
    }

    val fs = FileSystems.getDefault

    val cwd = fs.getPath(System.getProperty("user.dir"))
    val src = cwd.resolve(args(0))
    val dst = cwd.resolve(args(1))

    if (!src.toFile.exists()) {
      println(s"'$src' does not exist!}")
      sys.exit(1)
    }

    GitSubmoduleMove(cwd, src, dst)
  }

  type SubmoduleProcessor = (Path, Path, Path) => Unit

  def exec(repoRoot: Path, src: Path, dst: Path)(fs: SubmoduleProcessor*) {
    fs foreach { _(repoRoot, src, dst) }
  }

  def repoRelativePath(repoRoot: Path, path: Path): Path = repoRoot.relativize(path)

  def fs = FS.DETECTED

  def updateGitModules(root: Path, srcPath: Path, dstPath: Path) {
    val src = srcPath.toString
    val dst = dstPath.toString
    val modules = new FileBasedConfig(root / ".gitmodules" toFile, fs)
    modules.load()
    val kvps = modules.getNames("submodule", src).asScala map { name => name -> modules.getString("submodule", src, name) }
    for ((name, value) <- kvps) {
      modules.setString("submodule", dst, name, value)
    }
    modules.setString("submodule", dst, "path", dst)
    modules.unsetSection("submodule", src)
    modules.save()
  }

  def moveSubmoduleGitDirectory(root: Path, src: Path, dst: Path) {
    val gitModulesDir = root / ".git/modules"
    val srcGitModuleDir = gitModulesDir / src
    val dstGitModuleDir = gitModulesDir / dst
    Files.createDirectories(dstGitModuleDir.getParent)
    Files.move(srcGitModuleDir, dstGitModuleDir)
  }

  def moveSubmoduleDirectory(root: Path, src: Path, dst: Path) {
    val absoluteSrc = root / src
    val absoluteDst = root / dst
    Files.createDirectories(absoluteDst.getParent)
    Files.move(absoluteSrc, absoluteDst)
  }

  def updateGitModuleConfig(root: Path, src: Path, dst: Path) {
    val config = new FileBasedConfig(root / ".git/modules" / dst / "config" toFile, fs)
    config.load()
    config.setString("core", null, "worktree", (root / "git/modules" / dst).relativize(root / dst).toString)
    config.save()
  }

  def updateGitDir(root: Path, src: Path, dst: Path) {
    val newPath = (root / dst).relativize(root / ".git/modules" / dst)
    val pw = new PrintWriter(root / dst / ".git" toFile)
    try { pw.println(s"gitdir: $newPath") }
    finally { pw.close() }
  }

  def apply(cwd: Path, src: Path, dst: Path) {
    val repo = new FileRepositoryBuilder().setGitDir(cwd / ".git" toFile).readEnvironment().setMustExist(true).build()
    val repoRoot = FileSystems.getDefault.getPath(repo.getWorkTree.getPath)

    val List(relativeSrc, relativeDst) = List(src, dst) map repoRoot.relativize

    exec(repoRoot, relativeSrc, relativeDst)(
      updateGitModules,
      moveSubmoduleGitDirectory,
      moveSubmoduleDirectory,
      updateGitModuleConfig,
      updateGitDir
    )

    new AddCommand(repo).addFilepattern(".gitmodules").call()
    new RmCommand(repo).setCached(true).addFilepattern(relativeSrc.toString).call()
    new AddCommand(repo).addFilepattern(relativeDst.toString).call()
    repo.close()
  }
}
