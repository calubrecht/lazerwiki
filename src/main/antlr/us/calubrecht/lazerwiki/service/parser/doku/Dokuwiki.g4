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

all_char
  : CHARACTER | WS | header_tok
  ;

header
  : header_tok all_char+ header_tok
  ;

line
  : all_char*
  ;


