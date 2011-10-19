@Grab( 'net.sf.opencsv:opencsv:2.3' )
import au.com.bytecode.opencsv.CSVReader

/**
 * Groovy script converting YouTrack CVS export results into MediaWiki tables.
 * Usage: groovy y2m.groovy <YouTrack URL> <CSV file> [<Fields>] [<Group-By-Field>]
 */

List<String> defaultFields       = [ 'Issue Id', 'Type', 'Summary' ]
String       defaultGroupByField = 'Type'

if ( args.length < 2 )
{
    System.err.println """
------------------------------------------------------------------------------------------------------------------------------
Usage: groovy y2m.groovy <YouTrack URL> <CSV file> [<Fields>] [<Group-By-Field>]
------------------------------------------------------------------------------------------------------------------------------
YouTrack URL   - Base URL of YouTrack application, like "http://youtrack.jetbrains.net" or "http://evgeny-goldin.org/youtrack"
CSV file       - "Issues in CSV"-exported file from YouTrack
Fields         - (optional) Comma-separated list of fields to use in MediaWiki table, "${ defaultFields.join( ',' )}" by default
Group-By-Field - (optional) Group MediaWiki table rows by this field, "$defaultGroupByField" if default 'Fields' are used
------------------------------------------------------------------------------------------------------------------------------
"""
    System.exit( 1 )
}


String       youTrackUrl  = args[ 0 ].replaceFirst( /(\\|\/)*$/, '' )
File         f            = new File( args[ 1 ] ).canonicalFile
List<String> fields       = ( args.size() > 2 ) ? args[ 2 ].split( ',' )*.trim().grep() : defaultFields
String       groupByField = ( args.size() > 3 )          ? args[ 3 ]           :
                            ( fields.is( defaultFields ))? defaultGroupByField :
                                                           ''
assert youTrackUrl && f.file && fields
assert ( ! groupByField ) || fields.contains( groupByField ), "Fields $fields don't contain \"$groupByField\" field"

List<String[]> lines  = new CSVReader( new FileReader( f )).readAll()
assert         lines?.size() > 1 , "No CSV data found in [$f]"

// https://sourceforge.net/tracker/?func=detail&aid=3425997&group_id=148905&atid=773541
lines = lines.findAll{ it.size() == lines[ 0 ].size() }

/**
 * Mapping of fields to their corresponding indices in each String[]:  "Issue Id" => 0, "Project" => 1, "Tags" => 2, etc.
 */
Map<String, Integer> fieldsMapped = lines[ 0 ].inject( [:] ){ Map m, String field -> m[ field ] = m.size(); m }
fieldsMapped.keySet().with {
    assert containsAll( fields ), "CSV file [$f] contains $delegate fields, but doesn't contain ${ fields - intersect( fields )} fields"
}

List<String[]> linesGrouped = lines[ 1 .. -1 ]
assert         linesGrouped
linesGrouped.each{ String[] line -> assert ( line.size() == lines[ 0 ].size()) }

if ( groupByField )
{
    int fieldIndex = fieldsMapped[ groupByField ]
    assert (( fieldIndex > -1 ) && ( fieldIndex < lines[ 0 ].size()))

    /**
     * Mapping of issue field, like 'Feature' or 'Bug' type, to list of lines.
     * Each String[] in the list represents a single issue of the corresponding type.
     */
    Map<String, List<String[]>> linesMapped = linesGrouped.inject( [:].withDefault { [] } ){
        Map m, String[] line ->
        String fieldValue = line[ fieldIndex ]
        assert fieldValue, "Field \"$groupByField\" is not defined in line $line"
        m[ fieldValue ] << line
        m
    }

    /**
     * List of lines, created by iterating over sorted issue types.
     */
    linesGrouped = (( Set<String> ) linesMapped.keySet()).sort().inject( [] ){
        List result, String fieldValue -> result.addAll( linesMapped[ fieldValue ] )
        result
    }

    assert linesGrouped && linesGrouped.every { String[] line -> line.size() == lines[ 0 ].size() }
}


/**
 * MediaWiki table template
 */
String template = '''
{| border="1" cellspacing="0" cellpadding="5" class="wikitable" width="90%"
|-<% for ( field in fields ){ %>
| $field<% } %>
|-<% for ( line in lines ){ for ( field in fields ){ %>
| <%= line[ fieldsMapped[ field ]] %><% } %>
|-<% } %>
|}'''

println new groovy.text.GStringTemplateEngine().
        createTemplate( template ).
        make([ baseUrl      : youTrackUrl,
               fields       : fields,
               fieldsMapped : fieldsMapped,
               lines        : linesGrouped ])
