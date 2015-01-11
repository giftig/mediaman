package com.programmingcentre.mediaman.media

class NoSuchProgrammeException(msg: String) extends RuntimeException(msg)
class FileTooLargeException(msg: String) extends java.io.IOException(msg)
