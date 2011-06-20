eventCompileEnd = {
    def scriptDir = 'database/migration'

    ant.mkdir(dir: "${grailsSettings.baseDir}/${scriptDir}")
    ant.copy(todir: grailsSettings.classesDir) {
        fileset(dir: grailsSettings.baseDir) {
            include(name: "${scriptDir}/**/*.groovy")
            include(name: "${scriptDir}/**/*.sql")
        }
    }
}