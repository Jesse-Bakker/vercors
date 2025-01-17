lexer grammar SpecLexer;

/*
These tokens overlap one of the frontends, which leads to problems in ANTLR. They must be reproduced in the lexer of a
frontend, if the lexer of the frontend does not already define the token.

COMMA: ',';
SEMI: ';';
BLOCK_OPEN: '{';
BLOCK_CLOSE: '}';
PAREN_OPEN: '(';
PAREN_CLOSE: ')';
BRACK_OPEN: '[';
BRACK_CLOSE: ']';
ANGLE_OPEN: '<';
ANGLE_CLOSE: '>';
EQ: '=';
EQUALS: '==';
EXCL: '!';
STAR: '*';
PIPE: '|';
PLUS: '+';
COLON: ':';
VAL_INLINE: 'inline';
VAL_ASSERT: 'assert';
VAL_TRUE: 'true';
VAL_FALSE: 'false';
VAL_SIZEOF: 'sizeof';
*/

// Must be able to contain identifiers from any frontend, so it's fine to over-approximate valid identifiers a bit.
LANG_ID_ESCAPE: '`' ~[`]+ '`';

VAL_RESOURCE: 'resource';
VAL_PROCESS: 'process';
VAL_FRAC: 'frac';
VAL_ZFRAC: 'zfrac';
VAL_BOOL: 'bool';
VAL_RATIONAL: 'rational';
VAL_SEQ: 'seq';
VAL_SET: 'set';
VAL_BAG: 'bag';
VAL_LOC: 'loc';
VAL_POINTER: 'pointer';

VAL_PURE: 'pure';
VAL_THREAD_LOCAL: 'thread_local';

VAL_WITH: 'with';
VAL_THEN: 'then';
VAL_GIVEN: 'given';
VAL_YIELDS: 'yields';

VAL_AXIOM: 'axiom';

VAL_MODIFIES: 'modifies';
VAL_ACCESSIBLE: 'accessible';
VAL_REQUIRES: 'requires';
VAL_ENSURES: 'ensures';
VAL_CONTEXT_EVERYWHERE: 'context_everywhere';
VAL_CONTEXT: 'context';
VAL_LOOP_INVARIANT: 'loop_invariant';
VAL_KERNEL_INVARIANT: 'kernel_invariant';
VAL_SIGNALS: 'signals';

VAL_CREATE: 'create';
VAL_QED: 'qed';
VAL_APPLY: 'apply';
VAL_USE: 'use';
VAL_DESTROY: 'destroy';
VAL_SPLIT: 'split';
VAL_MERGE: 'merge';
VAL_CHOOSE: 'choose';
VAL_FOLD: 'fold';
VAL_UNFOLD: 'unfold';
VAL_OPEN: 'open';
VAL_CLOSE: 'close';
VAL_ASSUME: 'assume';
VAL_INHALE: 'inhale';
VAL_EXHALE: 'exhale';
VAL_LABEL: 'label';
VAL_REFUTE: 'refute';
VAL_WITNESS: 'witness';
VAL_GHOST: 'ghost';
VAL_SEND: 'send';
VAL_WORD_TO: 'to';
VAL_RECV: 'recv';
VAL_FROM: 'from';
VAL_TRANSFER: 'transfer';
VAL_CSL_SUBJECT: 'csl_subject';
VAL_SPEC_IGNORE: 'spec_ignore';
VAL_ACTION: 'action';
VAL_ATOMIC: 'atomic';

VAL_REDUCIBLE: 'Reducible';
VAL_ABSTRACT_STATE: 'AbstractState';
VAL_ADDS_TO: 'AddsTo';
VAL_APERM: 'APerm';
VAL_ARRAYPERM: 'ArrayPerm';
VAL_BUILD_MAP: 'buildMap';
VAL_CARD_MAP: 'cardMap';
VAL_CONTRIBUTION: 'Contribution';
VAL_DISJOINT_MAP: 'disjointMap';
VAL_EQUALS_MAP: 'equalsMap';
VAL_FUTURE: 'Future';
VAL_GET_FROM_MAP: 'getFromMap';
VAL_GET_FST: 'getFst';
VAL_GET_OPTION: 'getOption';
VAL_GET_SND: 'getSnd';
VAL_HEAD: 'head';
VAL_HELD: 'held';
VAL_HIST: 'Hist';
VAL_HPERM: 'HPerm';
VAL_IDLE: 'idle';
VAL_IS_EMPTY: 'isEmpty';
VAL_ITEMS_MAP: 'itemsMap';
VAL_KEYS_MAP: 'keysMap';
VAL_PERM_VAL: 'perm';
VAL_PERM: 'Perm';
VAL_POINTS_TO: 'PointsTo';
VAL_REMOVE: 'remove';
VAL_REMOVE_AT: 'removeAt';
VAL_REMOVE_FROM_MAP: 'removeFromMap';
VAL_RUNNING: 'running';
VAL_SOME: 'Some';
VAL_TAIL: 'tail';
VAL_VALUE: 'Value';
VAL_VALUES_MAP: 'valuesMap';
VAL_GETOPTELSE: 'getOrElseOption';

UNFOLDING: '\\unfolding';
IN: '\\in';
MEMBEROF: '\\memberof';
CURRENT_THREAD: '\\current_thread';
FORALL_STAR: '\\forall*';
FORALL: '\\forall';
EXISTS: '\\exists';
LET: '\\let';
SUM: '\\sum';
LENGTH: '\\length';
OLD: '\\old';
TYPEOF: '\\typeof';
MATRIX: '\\matrix';
ARRAY: '\\array';
POINTER: '\\pointer';
POINTER_INDEX: '\\pointer_index';
VALUES: '\\values';
VCMP: '\\vcmp';
VREP: '\\vrep';
MSUM: '\\msum';
MCMP: '\\mcmp';
MREP: '\\mrep';
RESULT: '\\result';
LTID: '\\ltid';
GTID: '\\gtid';

NONE: 'none';
OPTION_NONE: 'None';
WRITE: 'write';
READ: 'read';
EMPTY: 'empty';

CONS: '::';
FRAC_DIV: '\\';
SEP_CONJ: '**';
IMPLIES: '==>';
WAND: '-*';
RANGE_TO: '..';
TRIGGER_OPEN: '{:';
TRIGGER_CLOSE: ':}';