/*
Closure addDependencies = { List dependencies ->
	for (Map depNotation in dependencies) {
	  add(
		depNotation['configurationName'],
		[
		  group: depNotation['group'],
		  name: depNotation['name'],
		  version: depNotation.get('version', 'latest.release')
		]
	  ) {
		depNotation.get('excludes', []).each { exclNotation ->
		  exclude(
			group: exclNotation['group'],
			module: exclNotation['module']
		  )
		}
	  }
	}
  }
*/
