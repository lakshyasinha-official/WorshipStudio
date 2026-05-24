// Mirrors ChordEngine.kt — all constants and logic must stay in sync.

export const ALL_KEYS  = ['C','C#','D','Eb','E','F','F#','G','Ab','A','Bb','B']
export const QUALITIES = ['Major', 'Minor', 'Diminished']
export const PART_TYPES = ['Start', 'Verse', 'Chorus', 'Bridge', 'Other', 'End']

// ── Scale tables (12-note chromatic) ──────────────────────────────────────────
const MAJOR_SCALES = {
  'C' : ["C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B"],
  'C#': ["C#", "D", "D#", "E", "E#", "F#", "G", "G#", "A", "A#", "B", "B#"],
  'D' : ["D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B", "C", "C#"],
  'Eb': ["Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B", "C", "Db", "D"],
  'E' : ["E", "F", "F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#"],
  'F' : ["F", "Gb", "G", "Ab", "A", "Bb", "B", "C", "Db", "D", "Eb", "E"],
  'F#': ["F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#", "E", "E#"],
  'G' : ["G", "Ab", "A", "Bb", "B", "C", "C#", "D", "Eb", "E", "F", "F#"],
  'Ab': ["Ab", "A", "Bb", "B", "C", "Db", "D", "Eb", "E", "F", "Gb", "G"],
  'A' : ["A", "Bb", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#"],
  'Bb': ["Bb", "B", "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A"],
  'B' : ["B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#"]
}

const MINOR_SCALES = {
  'C' : ["C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"],
  'C#': ["C#", "D", "D#", "E", "E#", "F#", "G", "G#", "A", "A#", "B", "B#"],
  'D' : ["D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B", "C", "C#"],
  'Eb': ["Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B", "C", "Db", "D"],
  'E' : ["E", "F", "F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#"],
  'F' : ["F", "Gb", "G", "Ab", "A", "Bb", "B", "C", "Db", "D", "Eb", "E"],
  'F#': ["F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#", "E", "E#"],
  'G' : ["G", "Ab", "A", "Bb", "B", "C", "C#", "D", "Eb", "E", "F", "F#"],
  'Ab': ["Ab", "A", "Bb", "B", "C", "Db", "D", "Eb", "E", "F", "Gb", "G"],
  'A' : ["A", "Bb", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#"],
  'Bb': ["Bb", "B", "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A"],
  'B' : ["B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#"]
}

const HARMONIC_MINOR_SCALES = {
  'C' : ["C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"],
  'C#': ["C#", "D", "D#", "E", "E#", "F#", "G", "G#", "A", "A#", "B", "B#"],
  'D' : ["D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B", "C", "C#"],
  'Eb': ["Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B", "C", "Db", "D"],
  'E' : ["E", "F", "F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#"],
  'F' : ["F", "Gb", "G", "Ab", "A", "Bb", "B", "C", "Db", "D", "Eb", "E"],
  'F#': ["F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#", "E", "E#"],
  'G' : ["G", "Ab", "A", "Bb", "B", "C", "C#", "D", "Eb", "E", "F", "F#"],
  'Ab': ["Ab", "A", "Bb", "B", "C", "Db", "D", "Eb", "E", "F", "Gb", "G"],
  'A' : ["A", "Bb", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#"],
  'Bb': ["Bb", "B", "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A"],
  'B' : ["B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#"]
}

// ── Degree → [scaleIndex, suffix] ─────────────────────────────────────────────
const DEGREE_INFO = {
  // ── Major mode degrees ──────────────────────────────────
  'I'     : [0, ''],
  '♭II'   : [1, ''],
  'II'    : [2, ''],
  'ii'    : [2, 'm'],
  '♭III'  : [3, ''],
  'III'   : [4, ''],
  'iii'   : [4, 'm'],
  'IV'    : [5, ''],
  '♯IV'   : [6, ''],
  '♭V'    : [6, ''],
  'V'     : [7, ''],
  '♭VI'   : [8, ''],
  'VI'    : [9, ''],
  'vi'    : [9, 'm'],
  '♭VII'  : [10, ''],
  'VII'   : [11, ''],
  'vii°'  : [11, 'dim'],

  // ── Minor / Harmonic-minor mode degrees ─────────────────
  'i'      : [0, 'm'],
  'I'      : [0, ''],
  '♭ii'    : [1, ''],
  'ii'     : [2, 'm'],
  'II'     : [2, ''],
  'ii°'    : [2, 'dim'],
  '♭iii'   : [3, ''],
  'III'    : [3, ''],
  'iii'    : [4, 'm'],
  'iv'     : [5, 'm'],
  'IV'     : [5, ''],
  '♯iv'    : [6, 'm'],
  '♭v'     : [6, ''],
  'v'      : [7, 'm'],
  'V'      : [7, ''],
  '♭vi'    : [8, ''],
  'VI'     : [8, ''],
  'vi'     : [9, 'm'],
  '♭vii'   : [10, ''],
  'VII'    : [10, ''],
  'vii'    : [11, 'm'],
  'vii°'   : [11, 'dim']
}

function scaleFor(key, quality) {
  if (quality === 'Minor')      return MINOR_SCALES[key]
  if (quality === 'Diminished') return HARMONIC_MINOR_SCALES[key]
  return MAJOR_SCALES[key]
}

/** Resolve a Roman-numeral degree to a real chord name, e.g. "vi" in G-Major → "Em". */
export function resolveChord(degree, key, quality = 'Major') {
  if (degree.includes('/')) {
    const [basePart, bassPart] = degree.split('/')
    const base = resolveChord(basePart, key, quality)
    const bass = resolveBassNote(bassPart, key, quality)
    if (base !== basePart || bass !== bassPart) {
      return `${base}/${bass}`
    }
  }
  const scale = scaleFor(key, quality)
  if (!scale) return degree
  const info = DEGREE_INFO[degree]
  if (!info) return degree
  const [index, suffix] = info
  return scale[index] + suffix
}

function resolveBassNote(degree, key, quality) {
  // Try degree map
  const info = DEGREE_INFO[degree]
  if (info) {
    const scale = scaleFor(key, quality)
    return scale ? scale[info[0]] : degree
  }
  // Try numeric 1-7
  const num = parseInt(degree, 10)
  if (!isNaN(num) && num >= 1 && num <= 7) {
    const scale = scaleFor(key, quality)
    if (!scale) return degree
    const indexMap = { 1: 0, 2: 2, 3: 4, 4: 5, 5: 7, 6: 9, 7: 11 }
    return scale[indexMap[num]]
  }
  return degree
}

/** Return degree list appropriate for a given quality (matches Android degreesForQuality). */
export function degreesForQuality(quality) {
  if (quality === 'Minor')      return ["i", "♭ii", "ii", "♭iii", "iii", "iv", "♭v", "v", "♭vi", "vi", "♭vii", "vii"]
  if (quality === 'Diminished') return ["i", "ii°", "III", "iv", "V",  "VI", "vii°"]
  return                               ["I", "♭II", "ii", "♭III", "iii", "IV", "♯IV", "V", "♭VI", "vi", "♭VII", "vii°"]
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
