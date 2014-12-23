package com.programmingcentre.utils.media

class NoSuchProgrammeException(msg: String) extends RuntimeException(msg)
class FileTooLargeException(msg: String) extends java.io.IOException(msg)
