package utils

object Numbers {
  def isAllDigits(value: String) = value forall Character.isDigit
}
