package integration.`new`

import integration.helper.VercorsSpec

class AlgorithmExamplesSpec extends VercorsSpec {
  vercors should verify using silicon example "concepts/algo/BinarySearch.pvl"
  vercors should verify using silicon example "concepts/algo/DutchNationalFlag.pvl"
  vercors should verify using silicon example "concepts/algo/reach.pvl"
  vercors should verify using silicon example "concepts/algo/linkedlist.pvl"
  vercors should verify using silicon example "concepts/algo/list.pvl"
}