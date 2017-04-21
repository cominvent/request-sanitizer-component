# RequestSanitizerComponent
A search component for Solr to sanitize request parameter input

[![Build Status](https://travis-ci.org/cominvent/request-sanitizer-component.svg?branch=master)](https://travis-ci.org/cominvent/request-sanitizer-component)

## Build

Build with maven:

    mvn package

Copy the jar to a place where Solr can find it:

    SOLR_HOME=/path/to/solr/home
    mkdir $SOLR_HOME/lib
    cp target/RequestSanitizerComponent-1.0.jar $SOLR_HOME/lib/

## Install

Download a pre-built jar from [releases](https://github.com/cominvent/request-sanitizer-component/releases) section.
and drop it in your `$SOLR_HOME/lib/`

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

## Contributions

The component is licensed under [the Apache License](LICENSE), so you can
use it freely for anything :)

I hope to extend the component with other useful sanitizing features, see issue tracker.

Pull Requests welcome!

## ALPHA: Install usin bin/solr plugin install

**NB:** works only with unreleased build, see https://s.apache.org/solr-plugin):

    bin/solr plugin repo add cominvent https://github.com/cominvent/solr-plugins
    bin/solr plugin install request-sanitizer
