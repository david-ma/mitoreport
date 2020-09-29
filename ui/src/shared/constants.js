export const MIN_POS = 200

export const MAX_POS = 16300

export const MAX_READ_DEPTH = 10000

export const SAVE_INTERVAL_MS = 5000

export const DEFAULT_SNACKBAR_OPTS = {
  active: false,
  color: 'green',
  message: null,
  timeout: 3000,
}

export const CONSEQUENCE_NAMES = [
  { id: 'frameshift_variant', name: 'frameshift_variant' },
  { id: 'inframe_deletion', name: 'inframe_deletion' },
  { id: 'missense_variant', name: 'missense_variant' },
  { id: 'stop_gained', name: 'stop_gained' },
  { id: 'synonymous_variant', name: 'synonymous_variant' },
  { id: 'upstream_gene_variant', name: 'upstream_gene_variant' },
]

export const DEFAULT_VARIANT_SEARCH = {
  name: 'None',
  description: 'No filters applied',
  custom: false,
  filterConfig: {
    posRange: [0, 16300],
    allele: '',
    selectedTypes: [],
    selectedGenes: [],
    selectedConsequences: [],
    vafRange: [0, 1],
    depthRange: [0, 10000],
    disease: '',
    mitoMap: '',
    curatedRefs: '',
    hgvsp: '',
    hgvsc: '',
    hgvs: '',
  },
}