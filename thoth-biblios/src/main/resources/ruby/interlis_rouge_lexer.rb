# frozen_string_literal: true

require 'rouge'

module Rouge
  module Lexers
    class Interlis < RegexLexer
      title 'INTERLIS'
      desc 'INTERLIS data modelling language'
      tag 'interlis'
      aliases 'ili'
      filenames '*.ili'
      mimetypes 'text/x-interlis'

      KEYWORDS = %w[
        NOINCREMENTALTRANSFER COUNTERCLOCKWISE ANYSTRUCTURE AGGREGATION ASSOCIATION
        CARDINALITY CONSTRAINTS DERIVATIVES ENUMTREEVAL INHERITANCE RESTRICTION
        SUBDIVISION TRANSLATION UNQUALIFIED AGGREGATES ATTRIBUTES CONSTRAINT CONTINUOUS
        CONTRACTED HALIGNMENT INSPECTION METAOBJECT PROJECTION VALIGNMENT VERTEXINFO
        ACCORDING ATTRIBUTE CLOCKWISE EXISTENCE MANDATORY PARAMETER PERIPHERY REFERENCE
        REFSYSTEM STRUCTURE SYMBOLOGY TRANSIENT UNDEFINED ABSTRACT ANYCLASS CIRCULAR
        CONTINUE DEFERRED EXTENDED EXTERNAL FUNCTION GENERICS INTERLIS LINEATTR LINESIZE
        OPTIONAL OVERLAPS REQUIRED ROTATION THATAREA THISAREA TRANSFER CONTEXT CONTOUR
        DEFAULT DEFINED DEGREES DEPENDS DERIVED ENUMVAL EXTENDS GENERIC GRAPHIC IMPORTS
        OBJECTS ORDERED RADIANS TIDSIZE VERSION WITHOUT BASKET DOMAIN FORMAT HIDING
        LNBASE OBJECT OTHERS PARENT REFSYS UNIQUE VERTEX BASED BLANK CLASS EQUAL FINAL
        FIRST GRADS IDENT LOCAL MODEL TABLE TOPIC UNION WHERE XMLNS BASE CODE DIM1 DIM2
        FONT FORM FREE FROM JOIN LAST LINE LIST NAME NULL SIGN THIS TYPE UNIT VIEW WHEN
        WITH ALL AND ANY BAG END FIX I16 I32 NOT OID SET TID URI AS AT BY IN NO OF ON OR
        PI TO
      ].freeze

      TYPES = %w[
        ARCS AREA BINARY BLACKBOX BOOLEAN COORD COORD2 COORD3 DATE DATETIME DIRECTED
        MULTIAREA MULTICOORD MULTIPOLYLINE MULTISURFACE NUMERIC POLYLINE STRAIGHTS
        SURFACE TEXT MTEXT TIMEOFDAY XML
      ].freeze

      KEYWORDS_REGEX = /\b(?:#{KEYWORDS.map { |word| Regexp.escape(word) }.join('|')})\b/
      TYPES_REGEX = /\b(?:#{TYPES.map { |word| Regexp.escape(word) }.join('|')})\b/

      state :root do
        rule %r/\s+/, Text::Whitespace

        # Line comments starting with !!
        rule %r/!!.*/, Comment::Single

        # Block comments
        rule %r{/\*[\s\S]*?\*/}, Comment::Multiline

        # Strings with backslash escapes
        rule %r/"(?:\\.|[^"\\])*"/, Str::Double

        # Keywords
        rule KEYWORDS_REGEX, Keyword

        # Built-in INTERLIS types
        rule TYPES_REGEX, Name::Builtin

        # Numbers, translated from the Prism regexp
        rule %r/\b(?:\+|-)?(?:[1-9]\d*\.?|0\.)\d*(?:[Ee][+-]?[1-9]\d*)?\b/, Num

        # Operators and punctuation
        rule %r/--<#>/, Operator
        rule %r/--<>/, Operator
        rule %r/--/, Operator
        rule %r/[;=:]/, Punctuation

        # Fallback: consume one character to avoid infinite loops
        rule %r/./, Text
      end
    end
  end
end
