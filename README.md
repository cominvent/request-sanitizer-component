# RequestSanitizerComponent
A search component for Solr to sanitize request parameter input

## Build and install

Build with maven:

    mvn package

Or simply download a pre-built jar from [releases](https://github.com/cominvent/request-sanitizer-component/releases) section.

Copy the jar to a place where Solr can find it:

    SOLR_HOME=/path/to/solr/home
    mkdir $SOLR_HOME/lib
    cp target/RequestSanitizerComponent-1.0.jar $SOLR_HOME/lib/

### solrconfig.xml

Define the component:

    <searchComponent name="request-sanitizer" class="com.cominvent.solr.RequestSanitizerComponent"/>

Add the component as first-component to your `/select` handler:

    <arr name="first-components">
      <str>request-sanitizer</str>
    </arr>

Define sanitizing rules in `defaults` section of `/select` handler:

    <str name="sanitize">rows=>100:100</str>
    <str name="sanitize">offset=>10000:10000</str>

More examples of usage below

## Usage:
Add the sanitize request parameter in solrconfig.xml. Examples:

Always override the field, just like invariant

    sanitize=rows=25 or sanitize=rows=invariant:25

Map values to other values (if no match found, will use input value

    sanitize=echoParams=alle:all eksplisitt:explicit

Set default value if param is not set

    sanitize=debugQuery=default:true

Restrict numeric value to a max limit (if >100 then cap at 100)

    sanitize=rows=>100:100

Multiple replacements through multiple http params

    sanitize=rows=>100:100&sanitize=offset=>10000:10000


