# frozen_string_literal: true

require 'rouge'

module Rouge
  module Lexers
    class Ilimap < RegexLexer
      title 'ilimap'
      desc 'ilimap mapping DSL'
      tag 'ilimap'
      aliases 'ilimap'
      filenames '*.ilimap'
      mimetypes 'text/x-ilimap'

      KEYWORDS = %w[
        mapping v2 job input output oid basket enum defaults
        rule target source from class where join inner left
        identity assign bag ref association role required
        create loss metadata option connection query geometry
        driver url user password userEnv passwordEnv property
        topic basketId oidColumn sql columns column encoding
        type srid attribute structure mode embed expand
        maxItems parentRef parent sourcePath reasonCode
        description when direction roundtrip lossiness name
        modeldir path model format namespace failPolicy
        compileMode strategy
      ].freeze

      LITERALS = %w[ true false null ].freeze

      KEYWORDS_REGEX = /\b(?:#{KEYWORDS.map { |word| Regexp.escape(word) }.join('|')})\b/
      LITERALS_REGEX = /\b(?:#{LITERALS.map { |word| Regexp.escape(word) }.join('|')})\b/

      state :root do
        rule %r/\s+/, Text::Whitespace

        rule %r{//.*}, Comment::Single

        rule %r{/\*[\s\S]*?\*/}, Comment::Multiline

        rule %r/"(?:\\.|[^"\\])*"/, Str::Double

        rule KEYWORDS_REGEX, Keyword

        rule LITERALS_REGEX, Keyword::Constant

        rule %r/#[A-Za-z_][A-Za-z0-9_]*/, Name::Label

        rule %r/\b-?\d+\.?\d*\b/, Num

        rule %r/=>/, Operator
      end
    end
  end
end
