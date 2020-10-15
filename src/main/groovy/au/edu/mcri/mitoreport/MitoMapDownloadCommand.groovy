package au.edu.mcri.mitoreport

import groovy.util.logging.Slf4j
import picocli.CommandLine.Command
import picocli.CommandLine.Option

import javax.inject.Inject
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.Callable

@Slf4j
@Command(name = 'mito-map-download', description = 'MitoMap annotations file download', mixinStandardHelpOptions = true)
class MitoMapDownloadCommand implements Callable<Path> {

    @Option(names = ['-o', '--output'], arity = '1', required = false, description = 'Full path of output MitoMap annotations HTML page, defaults to $WORKING_DIR/mito_map_annotations_<iso_date>.html')
    Path outputPath

    @Inject
    MitoMapPolymorphismsLoader mitoMapLoader

    Path call() throws Exception {
        if (!outputPath) {
            String workingDir = System.getProperty('user.dir')
            String fileName = "mito_map_annotations_${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.html"

            outputPath = Paths.get(workingDir, fileName)
        }

        log.info("Downloading MitoMap annotations to ${outputPath.toString()}")
        mitoMapLoader.downloadPolymorphismsCoding(outputPath)

        return outputPath
    }
}