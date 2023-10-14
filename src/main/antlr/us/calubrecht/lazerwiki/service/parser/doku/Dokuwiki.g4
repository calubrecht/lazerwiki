grammar Dokuwiki;

@header {
package us.calubrecht.lazerwiki.service.parser.doku;
}


//lexer grammar DokuLexer;

NEWLINE
    : [\r\n]
    ;


HEADER1: '======' ;
HEADER2: '=====' ;
HEADER3: '====' ;
HEADER4: '===' ;
HEADER5: '==' ;

WS: [ \t] ;

LINK_START: '[[' ;
LINK_END: ']]' ;
PIPE: '|';

CHARACTER
    : ~[\r\n]
    ;

//parser grammar DokuParser;

//options { tokenVocab=DokuLexer; }

page
    : row* EOF
    ;


row:
  ( WS* header WS* | line ) NEWLINE
  ;

header_tok
   : HEADER1 | HEADER2 | HEADER3 | HEADER4 | HEADER5
   ;

link_target
  : (CHARACTER | WS)+
  ;

link_display
  :
  PIPE all_char*
  ;

link:
 LINK_START link_target link_display? LINK_END
 ;

 all_char
   : CHARACTER | WS | header_tok
   ;

inner_text
  :
    (all_char | link | PIPE) +
  ;

header
  : header_tok inner_text header_tok
  ;

line
  : inner_text
  ;


