# coremedia-tools

(c) Alexander Lenzen, Ahoi-IT. mailto-name: al, mailto-domain: ahoi-it.de

## Purpose

This is a CoreMedia command line tool package.

## Contents

### Zombie-Killer
The Zombie-Killer is a tool that allows you to kill Zombie content, i.e. undead content that cannot be deleted by
  calling `bin/cm recyclebin`, because the content is still referenced by old versions of some content.

A reason for this might be that you want to remove a *deprecated* ContentType from your CMS, but still have some
  instances of this type in your recycle bin, which cannot be deleted the normal way, because they are still referenced.

Before you can finally delete this ContentType, all of its instances have to be deleted! And the zombie-killer can help
  you to achieve this.

Other tools to follow...

#### Usage

`cm zombie-killer -u <user> [other options] [--id <id> | --query <query>] [--kill]`

#### Examples

* find, but do not kill versions that reference content with id 42:
`cm-tools/bin/cm zombie-killer -u admin -p admin -url http://localhost:41080/coremedia/ior --id 42`

* find, **and** kill versions that reference content with id 42:
`cm-tools/bin/cm zombie-killer -u admin -p admin -url http://localhost:41080/coremedia/ior --id 42 --kill`

* find, **and** kill versions that reference content with type "OldType":
`cm-tools/bin/cm zombie-killer -u admin -p admin -url http://localhost:41080/coremedia/ior --query "TYPE = OldType AND isDeleted" --kill`


## References

* Coremedia: http://www.coremedia.com