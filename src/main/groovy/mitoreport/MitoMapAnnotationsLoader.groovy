package mitoreport

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.MapConstructor
import groovy.transform.TupleConstructor
import groovy.util.logging.Slf4j
import groovyx.net.http.HttpBuilder

import javax.inject.Singleton
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING

@Slf4j
@TupleConstructor(includes = 'mitoMapHost, codingsPagePath, controlsPagePath')
@MapConstructor
@Singleton
class MitoMapAnnotationsLoader {

    static final Map<String, String> TITLE_TO_PROPERTY_NAMES = Collections.unmodifiableMap([
            'Position'                                                                                               : 'positionStr',
            'Locus'                                                                                                  : 'locusAnchor',
            'Nucleotide Change'                                                                                      : 'alleleChange',
            'Codon Number'                                                                                           : 'codonNumber',
            'Codon Position'                                                                                         : 'codonPosition',
            'Amino Acid Change'                                                                                      : 'aminoAcidChange',
            "GB Freq<span class='mark'>&Dagger;</span>"                                                              : 'gbFreqStr',
            "GB Freq<br><span style='white-space:nowrap;'>FL&nbsp;(CR)<span class='mark'>&ast;&Dagger;</span></span>": 'gbFreqStr',
            'GB Seqs'                                                                                                : 'gbSeqsAnchor',
            "GB Seqs<br><span style='white-space:nowrap;'>total&nbsp;(FL/CR)<span class='mark'>&ast;</span></span>"  : 'gbSeqsAnchor',
            'Curated References'                                                                                     : 'curatedRefsAnchor',
    ])

    String mitoMapHost = 'https://mitomap.org'
    String codingsPagePath = '/foswiki/bin/view/MITOMAP/PolymorphismsCoding'
    String controlsPagePath = '/foswiki/bin/view/MITOMAP/PolymorphismsControl'
    String diseasesPagePath = '/cgi-bin/disease.cgi'

    void downloadAnnotations(Path outputPath) {
        if (Files.notExists(outputPath) || outputPath.toFile().text.empty) {
            String codingsHtml = downloadPage("$mitoMapHost$codingsPagePath")
            String controlsHtml = downloadPage("$mitoMapHost$controlsPagePath")
            List<MitoMapAnnotation> codings = parsePolymorphismsHtmlPage(codingsHtml, 'CODING')
            List<MitoMapAnnotation> controls = parsePolymorphismsHtmlPage(controlsHtml, 'CONTROL')
            List<MitoMapAnnotation> allAnnotations = codings + controls

            String diseasesTsv = downloadPage("$mitoMapHost$diseasesPagePath")

            Map<String, Object> diseasesAnnotations = diseasesTsv.split(/\n/)
                    .tail()
                    .collectEntries { String line ->
//                        id	pos	ref	alt	aachange	homoplasmy	heteroplasmy	disease	status	pubmed_ids	gbcnt	gbfreq
//                        205	114	C	T	noncoding	+	-	BD-associated	Reported	19290059	229	0.44

                        def lineItems = line.split(/\t/)
                        Integer lineItemsSize = lineItems.size()
                        String pos = lineItemsSize > 1 ? lineItems[1] : null
                        String ref = lineItemsSize > 2 ? lineItems[2] : null
                        String alt = lineItemsSize > 3 ? lineItems[3] : null
                        String aaChange = lineItemsSize > 4 ? lineItems[4] : null
                        String homoplasmy = lineItemsSize > 5 ? lineItems[5] : null
                        String heteroplasmy = lineItemsSize > 6 ? lineItems[6] : null
                        String disease = lineItemsSize > 7 ? lineItems[7] : null
                        String mitoMapStatus = lineItemsSize > 8 ? lineItems[8] : null
                        String pubmedIds = lineItemsSize > 9 ? lineItems[9] : null
                        String gbCount = lineItemsSize > 10 ? lineItems[10] : null
                        String gbFreq = lineItemsSize > 11 ? lineItems[11] : null
                        String compactAllele = "$ref$pos$alt"
                        [(compactAllele): ['diseaseAaChange': aaChange, 'disease': disease, 'mitoMapStatus': mitoMapStatus]]
                    }

            allAnnotations.each { MitoMapAnnotation annotation ->
                def diseaseAnnotation = diseasesAnnotations.getOrDefault(annotation.compactAllele, [:])
                annotation.diseaseAaChange = diseaseAnnotation.getOrDefault('diseaseAaChange', null)
                annotation.disease = diseaseAnnotation.getOrDefault('disease', null)
                annotation.diseaseStatus = diseaseAnnotation.getOrDefault('mitoMapStatus', null)
            }

            String json = JsonOutput.prettyPrint(JsonOutput.toJson(allAnnotations))
            writeToFile(outputPath, json)
        } else {
            log.info("Skipping download, MitoMap Polymorphisms already exists at ${outputPath.toString()}")
        }
    }

    private List<MitoMapAnnotation> parsePolymorphismsHtmlPage(String htmlText, String regionType) {
        Pattern matchData = Pattern.compile(/"data":(\[\s*?\[.*?\]\])/, Pattern.DOTALL)
        Pattern matchColumns = Pattern.compile(/"columns": (.*?}])/, Pattern.DOTALL)
        def dataMatcher = htmlText =~ matchData
        String dataJson = dataMatcher[0][1]
        def columnsMatcher = htmlText =~ matchColumns
        String columnsJson = columnsMatcher[0][1]

        def data = new JsonSlurper().parse(dataJson.bytes)
        def columns = new JsonSlurper().parse(columnsJson.bytes)

        List<MitoMapAnnotation> result = data.collect { def row ->
            Map<String, String> transformedRow = ['mitoMapHost': mitoMapHost, 'regionType': regionType]
            columns.eachWithIndex { def column, int index ->
                String title = column.title?.trim() ?: ''
                String propertyName = TITLE_TO_PROPERTY_NAMES.get(title)
                String propertyValue = row[index]
                transformedRow["$propertyName".toString()] = propertyValue
            }

            new MitoMapAnnotation(transformedRow)
        }

        return result
    }

    static List<MitoMapAnnotation> getAnnotations(Path annotationsFilePath = Paths.get(System.getProperty('user.dir'), "mito_map_annotations_${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.json")) {
        if (annotationsFilePath == null || !Files.exists(annotationsFilePath)) {
            log.error("Annotations file ${annotationsFilePath?.toString()} does not exist.")

            return Collections.emptyList()
        }

        List<MitoMapAnnotation> result = new JsonSlurper()
                .parse(annotationsFilePath.toFile())
                .collect { def obj ->
                    new MitoMapAnnotation(obj)
                }

        return result
    }

    static String downloadPage(String pageUrl) {
        log.info("Downloading MitoMap page from $pageUrl")

        def respBytes = HttpBuilder.configure {
            request.uri = pageUrl
            request.headers['User-Agent'] = 'https://www.mcri.edu.au'
        }.get() {
            response.exception { Exception e ->
                log.error("Error downloading page $pageUrl", e)
                throw new RuntimeException(e)
            }
        }

        String resultHtml = new String(respBytes as byte[])

        return resultHtml
    }

    private static void writeToFile(Path outputPath, String fileContents) {
        Path temp = Files.createTempFile('mitoreport', null)
        temp.toFile().withWriter { BufferedWriter w ->
            w.write(fileContents)
        }

        Files.move(temp, outputPath, REPLACE_EXISTING)
    }
}