package utilities

import org.apache.commons.lang3.StringUtils

/**
 * Author: Eike Isermann
 * This is a SEMBA2 class
 */
object Search {


  def listContainsNoCase(list: List[String], str: String): Boolean ={
    var retval = false
    list.foreach(s => {
        if(StringUtils.containsIgnoreCase(s, str)){
          return true
        }
      }
    )
    retval
  }
}
