package com.troikoss.continuum_explorer.utils

import java.io.IOException

class RestrictedAccessException(val path: String) : IOException("Restricted access to $path")
