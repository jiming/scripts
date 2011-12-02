
/**
 * Groovy script converting GitHub flavored Markdown files to MediaWiki syntax.
 * Usage: groovy g2m.groovy <markdown file> <output file>.
 *
 * See
 * http://daringfireball.net/projects/markdown/syntax
 * http://github.github.com/github-flavored-markdown/
 */


assert ( args.size() == 2 ), 'Arguments expected: <markdown file> <output file>'

final  File mdFile  = new File( args[ 0 ] ).canonicalFile
final  File outFile = new File( args[ 1 ] ).canonicalFile
final  time         = System.currentTimeMillis()
assert mdFile.file

println "Converting [$mdFile] to [$outFile]"
assert outFile.parentFile.with { directory || mkdirs() }
outFile.write( convert( mdFile.getText( 'UTF-8' )), 'UTF-8' )
println "Converting [$mdFile] to [$outFile] - Done (${ System.currentTimeMillis() - time } ms)"


/**
 * Converts the text specified from GitHub flavored Markdown to MediaWiki syntax.
 * @param mdText GitHub Markdown text
 * @return MediaWiki syntax
 */
String convert( String mdText )
{
    mdText.
    // \t => 4 spaces
    replaceAll( /\t/, '    ' ).
    // ## Title => == Title ==
    replaceAll( /(?m)^\s*(#+)\s+(.+)$/, { "${ '=' * it[ 1 ].size() } ${ it[ 2 ] } ${ '=' * it[ 1 ].size() }" }).
    // ![text](image link) => Message about having to upload the image
    replaceAll( /!\[(.+?)\]\((.+?)\)/, { "'''Image at \"${ it[ 2 ] }\" has to be uploaded. See [http://www.mediawiki.org/wiki/Manual:\$wgAllowExternalImages#Why_disallow_external_images.3F this] and [http://www.mediawiki.org/wiki/Help:Images this] for more details.'''" }).
    // [text](link) => '[link text]'
    replaceAll( /\[(.+?)\]\((.+?)\)/, "[\$2 \$1]" ).
    // [htpps:// => [htpp://
    replaceAll( /\[https:\/\//, '[http://' ).
    // code section (4 space-prepended lines) => <syntaxhighlight> section
    replaceAll( /(    .+?\r?\n)(((    .+?)|(\s*))\r?\n)*/, { "<syntaxhighlight lang=\"text\">\n${ trimCode( it[ 0 ] ) }</syntaxhighlight>\n\n\n" }).
    // `text` => '<code>'''text'''</code>'
    replaceAll( /`(.+?)`/, "<code>'''\$1'''</code>" ).
    // **text** => '''text'''
    replaceAll( /\*\*(.+?)\*\*/, "'''\$1'''" ).
    // __text__ => '''text'''
    replaceAll( /__(.+?)__/,  "'''\$1'''" ).
    // *text* => ''text''
    replaceAll( /\*(.+?)\*/,  "''\$1''"   ).
    // _text_ => ''text''
    replaceAll( /_(.+?)_/,    "''\$1''"   )
}


/**
 * Trims empty prefixes from the lines specified.
 * @param s lines to trim empty prefixes from.
 * @return  lines cleaned up
 */
String trimCode( String s )
{
    int prefix = s.findAll( /(?m)^(\s+)/ ){ it[ 1 ] }*.size().min()
    s.readLines().collect{ it.replaceAll( /^\s{$prefix}/, '' ) }.join( '\n' ) + '\n'
}