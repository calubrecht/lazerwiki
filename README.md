# lazerwiki
Java based wiki engine


A wiki engine built in java. I was disatisfied with managing a wiki with a php backend and the awkwardness of upgrading/maintaing such an application with changing php versions.
Operates wiht https://github.com/calubrecht/lazerwiki-ui as a react front end.

Initial implementation is interpretting Dokuwiki-style page syntax, with a DB backend, but alternate syntax support is being considered.
Goal is to support multiple wikis using a single deployed engine, based on URL.
