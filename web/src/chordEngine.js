// Mirrors ChordEngine.kt — all constants and logic must stay in sync.

export const ALL_KEYS  = ['C','C#','D','Eb','E','F','F#','G','Ab','A','Bb','B']
export const QUALITIES = ['Major', 'Minor', 'Diminished']
export const PART_TYPES = ['Start', 'Verse', 'Chorus', 'Bridge', 'Other', 'End']

// ── Scale tables ──────────────────────────────────────────────────────────────
const MAJOR_SCALES = {
  'C' : ['C' , 'D' , 'E' , 'F' , 'G' , 'A' , 'B' ],
  'C#': ['C#', 'D#', 'E#', 'F#', 'G#', 'A#', 'B#'],
  'D' : ['D' , 'E' , 'F#', 'G' , 'A' , 'B' , 'C#'],
  'Eb': ['Eb', 'F' , 'G' , 'Ab', 'Bb', 'C' , 'D' ],
  'E' : ['E' , 'F#', 'G#', 'A' , 'B' , 'C#', 'D#'],
  'F' : ['F' , 'G' , 'A' , 'Bb', 'C' , 'D' , 'E' ],
  'F#': ['F#', 'G#', 'A#', 'B' , 'C#', 'D#', 'E#'],
  'G' : ['G' , 'A' , 'B' , 'C' , 'D' , 'E' , 'F#'],
  'Ab': ['Ab', 'Bb', 'C' , 'Db', 'Eb', 'F' , 'G' ],
  'A' : ['A' , 'B' , 'C#', 'D' , 'E' , 'F#', 'G#'],
  'Bb': ['Bb', 'C' , 'D' , 'Eb', 'F' , 'G' , 'A' ],
  'B' : ['B' , 'C#', 'D#', 'E' , 'F#', 'G#', 'A#'],
}

const MINOR_SCALES = {
  'C' : ['C' , 'D' , 'Eb', 'F' , 'G' , 'Ab', 'Bb'],
  'C#': ['C#', 'D#', 'E' , 'F#', 'G#', 'A' , 'B' ],
  'D' : ['D' , 'E' , 'F' , 'G' , 'A' , 'Bb', 'C' ],
  'Eb': ['Eb', 'F' , 'Gb', 'Ab', 'Bb', 'B' , 'Db'],
  'E' : ['E' , 'F#', 'G' , 'A' , 'B' , 'C' , 'D' ],
  'F' : ['F' , 'G' , 'Ab', 'Bb', 'C' , 'Db', 'Eb'],
  'F#': ['F#', 'G#', 'A' , 'B' , 'C#', 'D' , 'E' ],
  'G' : ['G' , 'A' , 'Bb', 'C' , 'D' , 'Eb', 'F' ],
  'Ab': ['Ab', 'Bb', 'B' , 'Db', 'Eb', 'E' , 'Gb'],
  'A' : ['A' , 'B' , 'C' , 'D' , 'E' , 'F' , 'G' ],
  'Bb': ['Bb', 'C' , 'Db', 'Eb', 'F' , 'Gb', 'Ab'],
  'B' : ['B' , 'C#', 'D' , 'E' , 'F#', 'G' , 'A' ],
}

const HARMONIC_MINOR_SCALES = {
  'C' : ['C' , 'D' , 'Eb', 'F' , 'G' , 'Ab', 'B' ],
  'C#': ['C#', 'D#', 'E' , 'F#', 'G#', 'A' , 'B#'],
  'D' : ['D' , 'E' , 'F' , 'G' , 'A' , 'Bb', 'C#'],
  'Eb': ['Eb', 'F' , 'Gb', 'Ab', 'Bb', 'B' , 'D' ],
  'E' : ['E' , 'F#', 'G' , 'A' , 'B' , 'C' , 'D#'],
  'F' : ['F' , 'G' , 'Ab', 'Bb', 'C' , 'Db', 'E' ],
  'F#': ['F#', 'G#', 'A' , 'B' , 'C#', 'D' , 'E#'],
  'G' : ['G' , 'A' , 'Bb', 'C' , 'D' , 'Eb', 'F#'],
  'Ab': ['Ab', 'Bb', 'B' , 'Db', 'Eb', 'E' , 'G' ],
  'A' : ['A' , 'B' , 'C' , 'D' , 'E' , 'F' , 'G#'],
  'Bb': ['Bb', 'C' , 'Db', 'Eb', 'F' , 'Gb', 'A' ],
  'B' : ['B' , 'C#', 'D' , 'E' , 'F#', 'G' , 'A#'],
}

// ── Degree → [scaleIndex, suffix] ─────────────────────────────────────────────
const DEGREE_INFO = {
  // Major mode
  'I'   : [0, ''   ],
  'ii'  : [1, 'm'  ],
  'iii' : [2, 'm'  ],
  'IV'  : [3, ''   ],
  'V'   : [4, ''   ],
  'vi'  : [5, 'm'  ],
  'vii°': [6, 'dim'],
  'vii' : [6, 'dim'],
  // Minor / harmonic minor mode
  'i'   : [0, 'm'  ],
  'ii°' : [1, 'dim'],
  'III' : [2, ''   ],
  'iv'  : [3, 'm'  ],
  'v'   : [4, 'm'  ],
  'VI'  : [5, ''   ],
  'VII' : [6, ''   ],
}

function scaleFor(key, quality) {
  if (quality === 'Minor')      return MINOR_SCALES[key]
  if (quality === 'Diminished') return HARMONIC_MINOR_SCALES[key]
  return MAJOR_SCALES[key]
}

/** Resolve a Roman-numeral degree to a real chord name, e.g. "vi" in G-Major → "Em". */
export function resolveChord(degree, key, quality = 'Major') {
  const scale = scaleFor(key, quality)
  if (!scale) return degree
  const info = DEGREE_INFO[degree]
  if (!info) return degree
  const [index, suffix] = info
  return scale[index] + suffix
}

/** Return degree list appropriate for a given quality (matches Android degreesForQuality). */
export function degreesForQuality(quality) {
  if (quality === 'Minor')      return ['i', 'ii°', 'III', 'iv', 'v',  'VI', 'VII']
  if (quality === 'Diminished') return ['i', 'ii°', 'III', 'iv', 'V',  'VI', 'vii°']
  return                               ['I', 'ii',  'iii', 'IV', 'V',  'vi', 'vii°']
}

/**
 * Parse lyrics containing [degree] markers into an array of tokens:
 *   { type: 'text',  content: '...' }
 *   { type: 'chord', degree: '...', resolved: '...' }
 */
export function parseLyrics(lyrics, key, quality = 'Major') {
  const tokens = []
  const regex = /\[([^\]]+)\]/g
  let lastEnd = 0
  let match
  while ((match = regex.exec(lyrics)) !== null) {
    if (match.index > lastEnd) {
      tokens.push({ type: 'text', content: lyrics.substring(lastEnd, match.index) })
    }
    const degree = match[1]
    tokens.push({ type: 'chord', degree, resolved: resolveChord(degree, key, quality) })
    lastEnd = match.index + match[0].length
  }
  if (lastEnd < lyrics.length) {
    tokens.push({ type: 'text', content: lyrics.substring(lastEnd) })
  }
  return tokens
}
