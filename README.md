# Underwood
This project is an Annotation processor that takes Java class's and creates Gson TypeAdapters that can serialize and deserialize them to and from JSON.

##Purpose
Right to if developers want to use Gson, then for each Java Object they wish to convert to/from JSON they need to also write TypeAdapters. This leads to allot of boilerplate code that many feel is unnecessary. 

One option is to allow Gson to write its own type adapters for you at runtime using reflection, this can be at the cost of performance, especially for mobile devs. Another option, similar to this library is to use an AutoValue extension aptly named AutoValue.Gson, This then requires you to use AutoValue for all objects which for existing projects could be a large task. AutoValue.Gson makes the TypeAdapters the same time it builds your AutoValue classes, meaning reflection is moved from runtime to compile time. 

This Library hopes to, like AutoValue.Gson, move the reflection to compile time and allow you to avoid all of that boilerplate code. Hopefully with more universal support for teams where not all objects are using AutoValue.

##Features Coming soon
* Public interface  to register TypeAdapters
* Support for JSON Arrays
* Support for objects Annotated with @Autovalue
