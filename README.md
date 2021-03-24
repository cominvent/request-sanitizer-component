# RequestSanitizerComponent
A search component for Solr to sanitize request parameter input

[![Java CI with Maven](https://github.com/cominvent/request-sanitizer-component/actions/workflows/maven.yml/badge.svg)](https://github.com/cominvent/request-sanitizer-component/actions/workflows/maven.yml)

## Install

The component is installed using Solr's package manager:

1. Start Solr with package manager enabled

       # You need to set Java property 'enable.packages=true', e.g.
       bin/solr -c -Denable.packages=true

2. Install this plugin repository into your Solr cluster

       bin/solr package add-repo cominvent https://raw.githubusercontent.com/cominvent/solr-plugins/master

3. Install the package

       # First confirm that the package is in the list
       bin/solr package list-available
       # Install and deploy the plugin
       bin/solr package install request-sanitizer
       # Deploy the package to your collection(s)
       bin/solr package deploy request-sanitizer -y -collections mycoll  

## Configuration

After install and deploy, the component is installed and ready to use.
What remains is to configure your Request Handler(s). In `solrconfig.xml`:

1. Add the component as first-component to your `/select` handler:

       
       <arr name="first-components">
         <str>request-sanitizer</str>
       </arr>

2. Define sanitizing rules in `defaults` section of `/select` handler:
   These are examples of rules you can apply:

       <str name="sanitize">rows=>100:100</str>
       <str name="sanitize">offset=>10000:10000</str>

### Available rules

Always override the field, just like invariant

    sanitize=rows=25 or sanitize=rows=invariant:25

Map values to other values (if no match found, will use input value):

    sanitize=echoParams=alle:all eksplisitt:explicit

Set default value if param is not set

    sanitize=debugQuery=default:true

Restrict numeric value to a max limit (if >100 then cap at 100)

    sanitize=rows=>100:100

Multiple replacements through multiple http params

    sanitize=rows=>100:100&sanitize=offset=>10000:10000

## Build

Build with maven:

    mvn package

Copy the jar to a place where Solr can find it:

    SOLR_HOME=/path/to/solr/home
    mkdir $SOLR_HOME/lib
    cp target/request-sanitizer-*.jar $SOLR_HOME/lib/

## Contributions

The component is licensed under [the Apache License](LICENSE), so you can
use it freely for anything :)

I hope to extend the component with other useful sanitizing features, see issue tracker.

Pull Requests welcome!

## Manual install

This is an alaternative way to install, if you don't want to use package manager:

Download a pre-built jar from [releases](https://github.com/cominvent/request-sanitizer-component/releases) section.
and drop it in your `$SOLR_HOME/lib/`

Then define the component in `solrconfig.xml`:

    <searchComponent name="request-sanitizer" class="com.cominvent.solr.RequestSanitizerComponent"/>

Now you can configure the component as above