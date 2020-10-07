package au.edu.mcri.mitoreport

import gngs.Cli
import gngs.ToolBase
import gngs.Utils
import gngs.VCF
import gngs.Variant
import graxxia.CSV
import graxxia.TSV
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log

import static gngs.VEPConsequences.RANKED_CONSEQUENCES

/**
 * Creates a comprehensive report for interpreting Mitochondrial variants based on:
 *
 * - output VCF from Broad Mitochondrial analysis pipeline
 * - annotations from VEP
 * - coverage plot computed by DeletionPlot tool
 *
 * @author Simon Sadedin
 */
@Log
class Report extends ToolBase {

    VCF vcf

    Map deletion

    File variantsResultJson

    @Override
    public void run() {

        log.info "Loading vcf $opts.vcf"
        vcf = VCF.parse(opts.vcf)

        log.info "Loaded ${vcf.size()} variants from $opts.vcf"

        deletion = new JsonSlurper().parse(new File(opts.del))

        log.info "Parsed deletion report $opts.del"

        log.info "Parsing annotations from $opts.ann"

        writeAnnotations(opts.o)
    }

    void writeAnnotations(String dir) {

        log.info "Loading annotations from $opts.ann"
        Map<String,Map> annotations = new CSV(opts.ann).toListMap().collectEntries { [it.Allele, it ] }
        log.info "Loaded ${annotations.size()} functional annotations"
        
        Map<String,Map> freqInfo = new TSV(opts.freq).toListMap().collectEntries { line ->
            def (ref,alt) = line.Change.tokenize('-')
            [ ref + line.Position + alt,  line] 
        }
        log.info "Loaded ${freqInfo.size()} frequency annotations"

        List results = []

        vcf.each { Variant v ->

            int sampleIndex = 0

            String compactAllele = v.ref.toUpperCase() + v.pos + v.alt.toUpperCase()
            String change = v.ref.toUpperCase() + '-' + v.alt.toUpperCase()

            Map variantInfo = [
                chr : v.chr,
                pos: v.pos,
                ref: v.ref,
                alt : v.alt,
                type: v.type,
                qual: v.qual,
                dosages: v.getDosages()
            ]

            Map vep = v.maxVep

            def consequencesWithRank = RANKED_CONSEQUENCES.withIndex(1).collect { String consequence, Integer index -> [id: consequence, rank: index] }
            Map vepInfo = [
                symbol: vep.SYMBOL,
                consequence: consequencesWithRank.find { it.id == vep.Consequence },
                hgvsp: URLDecoder.decode(vep.HGVSp?.replaceAll('^.*:',''), "UTF-8"),
                hgvsc: vep.HGVSc?.replaceAll('^.*:','')
            ]

            Map variantAnnotations =
                annotations.getOrDefault(compactAllele, [:]).collectEntries { key, value ->
                    def result = value
                    if(value instanceof String)
                        result = value.tokenize('|+').grep { it != 'NA' }*.trim().join(', ')

                    [key, result]
                }

            variantAnnotations.GBFreq = freqInfo[compactAllele]?.GBFreq?:0.0d

            Map infoField = v.parsedInfo
            infoField.remove('ANN')

            results << variantInfo + vepInfo + variantAnnotations + infoField + [genotypes: v.parsedGenotypes]
        }

        String json = JsonOutput.prettyPrint(JsonOutput.toJson(results))

        File dirFile = new File(dir)
        if(!dirFile.exists()) {
            log.info "Creating directory $dir"
            dirFile.mkdirs()
        }

        variantsResultJson = new File("$dir/variants.json")
        Utils.writer(variantsResultJson).withWriter {  it << json; it << '\n' }

        log.info "Wrote annotated variants to $variantsResultJson"
    }


    static void main(String[] args) {
        cli('Report -o <report dir> -vcf <vcf> [-vcf <vcf2>]... -del <del> [-del <del2>]...', 'Mitochondrial Analysis Report', args) {
            o 'Directory to save report assets to', args:1, required: true
            vcf 'VCF file for sample', args: Cli.UNLIMITED, required: true
            del 'Deletion analysis for sample', args: Cli.UNLIMITED, required: true
            ann 'Annotation file to apply to VCF', args: 1, required: true
            freq 'File containing mitomap genbank frequencies in the GBFreq column', args: 1, required: true
        }
    }
}
