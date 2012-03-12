require 'rspec/core/rake_task'
require 'lib/version'

QEDZIPFILE = "qedserver_#{QED_SERVER_VERSION.gsub(".","-")}.zip"
SERVER = "napcs:~/qedserver.napcs.com"

desc "Bundle the gems this thing needs"
task :bundle do
  sh "jruby -S bundle"
end

desc 'Default: run specs.'
task :default => :spec

desc "Run specs"
RSpec::Core::RakeTask.new do |t|
  t.pattern = "./spec/**/*_spec.rb" # don't need this, it's default.
  # Put spec opts in a file named .rspec in root
end

desc "Create customized version of Jetty for our use by copying only what we need."
task :build_jetty do
  FileUtils.rm_rf "sandbox"
  FileUtils.mkdir "sandbox"
  FileUtils.mkdir "sandbox/webserver"
  FileUtils.mkdir "sandbox/webserver/etc"
  FileUtils.mkdir "sandbox/webserver/resources"
  FileUtils.cp "jetty/start.jar", "sandbox/webserver/start.jar"
  FileUtils.cp_r "jetty/lib", "sandbox/webserver/lib"
  FileUtils.cp_r "jetty/LICENSES", "sandbox/webserver/LICENSES"
  FileUtils.cp "jetty/etc/jetty.xml", "sandbox/webserver/etc/jetty.xml"
  FileUtils.cp "jetty/etc/webdefault.xml", "sandbox/webserver/etc/webdefault.xml"
  FileUtils.cp "jetty/etc/realm.properties", "sandbox/webserver/etc/realm.properties"
  FileUtils.mkdir_p "sandbox/webserver/logs"
end

desc "inject QEDServer into Jetty"
task :install_qed do
  puts "Configuring folders for QEDServer"
  puts "patching logger"
  FileUtils.cp "jetty_config/slf4j-log4j12-1.6.1.jar", "sandbox/webserver/lib/slf4j-log4j12-1.6.1.jar"
  FileUtils.cp "jetty_config/log4j-1.2.16.jar", "sandbox/webserver/lib/log4j-1.2.16.jar"
  FileUtils.cp "jetty_config/slf4j-api-1.6.1.jar", "sandbox/webserver/lib/slf4j-api-1.6.1.jar"
  FileUtils.cp "jetty_config/log4j.properties", "sandbox/webserver/resources/log4j.properties"
  puts "updating startup scripts"
  FileUtils.cp_r "jetty_config/server.bat", "sandbox"
  FileUtils.cp_r "jetty_config/fresh_server.bat", "sandbox"
  FileUtils.cp_r "jetty_config/server.command", "sandbox"
  FileUtils.cp_r "jetty_config/fresh_server.command", "sandbox"
  FileUtils.cp_r "jetty_config/server.sh", "sandbox"
  FileUtils.cp_r "jetty_config/fresh_server.sh", "sandbox"
  puts "Copying QEDServer..."
  FileUtils.rm_rf "sandbox/webserver/contexts/"
  FileUtils.rm_rf "sandbox/webserver/webapps/"
  FileUtils.mkdir "sandbox/webserver/contexts"
  FileUtils.mkdir "sandbox/webserver/webapps"
  FileUtils.cp "jetty_config/webserver.xml", "sandbox/webserver/contexts/"
  FileUtils.cp "qedserver.war", "sandbox/webserver/webapps/webserver.war"
  puts "QEDServer copied into the webserver folder"
end

desc "Package QEDServer as a zipfile"
task :package_jetty => [:war, :build_jetty, :install_qed] do
 
  FileUtils.cp "END_USER_README.md", "sandbox/README.txt"
  %w{LICENSE HISTORY.txt}.each do |f|
    FileUtils.cp f, "sandbox/#{f}"
  end
  
  puts "Creating #{QEDZIPFILE}..."
  Dir.chdir "sandbox" do
     `zip -9 -r #{QEDZIPFILE} *`
  end
  FileUtils.mv "sandbox/#{QEDZIPFILE}", QEDZIPFILE
  puts "Done."
end

desc "Create the QEDServer war file"
task :war do
  sh "jruby -S warble compiled gemjar war"
end


namespace :release do 
  
  desc "update the web site"
  task :website do
    `scp html/* #{SERVER}`
  end

  desc "release new version"
  task :qed => :package_jetty do
    `scp #{QEDZIPFILE} #{SERVER}/#{QEDZIPFILE}`
  end

end

desc "DEPRECATED: Create a zip file for distribution to end users using the Winstone approach"
task :package_winstone => :war_winstone do
  FileUtils.cp "END_USER_README.md", "README.txt"
  sh "zip -9 #{QEDZIPFILE} LICENSE HISTORY.txt README.txt server.sh server.bat fresh_server.sh fresh_server.bat webserver.war"
  FileUtils.rm "README.txt"
end

desc "create the war file with the Winstone servlet container" 
task :war_winstone do
  FileUtils.rm_rf "public"
  sh "jruby -S warble compiled gemjar executable war"
end

desc "DEPRECATED: create customized version of Jetty for our use by copying the original and removing files"
task :copy_and_fix_jetty  do
  FileUtils.rm_rf "sandbox"
  FileUtils.mkdir "sandbox"
  FileUtils.cp_r "jetty", "sandbox/webserver"
  puts "removing unnecessary folders from Jetty"
  files = ["examples", "javadoc", "project-website", "extras", "jxr", "patches", "contrib"]
  files.each do |file|
    puts "Removing #{file}"
    FileUtils.rm_rf "sandbox/webserver/#{file}"
  end
end
