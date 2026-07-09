Prism.languages.ilimap = {
    'comment': [
        {
            pattern: /\/\/.*/,
            greedy: true
        },
        {
            pattern: /\/\*[\s\S]*?\*\//,
            greedy: true
        }
    ],

    'string': {
        pattern: /"(?:\\.|[^"\\])*"/,
        greedy: true
    },

    'keyword': {
        pattern: /\b(mapping|v2|job|input|output|oid|basket|enum|defaults|rule|target|source|from|class|where|join|inner|left|identity|assign|bag|ref|association|role|required|create|loss|metadata|option|connection|query|geometry|driver|url|user|password|userEnv|passwordEnv|property|topic|basketId|oidColumn|sql|columns|column|encoding|type|srid|attribute|structure|mode|embed|expand|maxItems|parentRef|parent|sourcePath|reasonCode|description|when|direction|roundtrip|lossiness|name|modeldir|path|model|format|namespace|failPolicy|compileMode|strategy)\b/,
        greedy: true
    },

    'boolean': {
        pattern: /\b(true|false)\b/,
        alias: 'keyword'
    },

    'null': {
        pattern: /\bnull\b/,
        alias: 'keyword'
    },

    'number': {
        pattern: /\b-?\d+\.?\d*\b/,
        greedy: true
    },

    'hash-token': {
        pattern: /#[A-Za-z_][A-Za-z0-9_]*/,
        alias: 'symbol'
    },

    'operator': {
        pattern: /=>/,
        alias: 'important'
    }
};
