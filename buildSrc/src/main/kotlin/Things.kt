import java.nio.file.Path
import org.gradle.api.file.RegularFile

val RegularFile.asPath: Path
    get() = asFile.toPath()