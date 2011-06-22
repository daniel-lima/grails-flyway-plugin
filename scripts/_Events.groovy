def scriptDir = 'database/migration'

eventCompileStart = {
    ant.mkdir(dir: "${grailsSettings.baseDir}/${scriptDir}")
    ant.delete(dir: "${grailsSettings.classesDir}/${grailsSettings.baseDir}")
}


eventCompileEnd = {
    ant.javac(classpathref:'grails.compile.classpath', srcdir: "${scriptDir}", destdir: "${grailsSettings.classesDir}")
    
    ant.copy(todir: grailsSettings.classesDir) {
        fileset(dir: grailsSettings.baseDir) {
            include(name: "${scriptDir}/**/*.groovy")
            include(name: "${scriptDir}/**/*.java")
            include(name: "${scriptDir}/**/*.sql")
        }
    }
}