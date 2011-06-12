require 'rspec/core/rake_task'
require 'lib/version'

QEDZIPFILE = "qedserver_#{QED_SERVER_VERSION.gsub(".","-")}.zip"
SERVER = "napcs:~/qedserver.napcs.com"

desc "Bundle the gems this thing needs"
task :bundle do
  sh "jruby -S bundle"
end

desc "Create a zip file for distribution to end users"
task :package_winstone => :war_exec do
  FileUtils.cp "END_USER_README.md", "README.txt"
  sh "zip -9 #{QEDZIPFILE} LICENSE HISTORY.txt README.txt server.sh server.bat fresh_server.sh fresh_server.bat webserver.war"
  FileUtils.rm "README.txt"
end

desc 'Default: run specs.'
task :default => :spec

desc "Run specs"
RSpec::Core::RakeTask.new do |t|
  t.pattern = "./spec/**/*_spec.rb" # don't need this, it's default.
  # Put spec opts in a file named .rspec in root
end

desc "create customized version of Jetty for our use"
task :config_jetty  do
  FileUtils.rm_rf "sandbox"
  FileUtils.mkdir "sandbox"
  FileUtils.cp_r "jetty", "sandbox/webserver"
  FileUtils.rm_rf "sandbox/webserver/contexts/"
  FileUtils.rm_rf "sandbox/webserver/webapps/"
  FileUtils.mkdir "sandbox/webserver/contexts"
  FileUtils.mkdir "sandbox/webserver/webapps"
  FileUtils.cp "jetty_config/slf4j-log4j12-1.6.1.jar", "sandbox/webserver/lib/slf4j-log4j12-1.6.1.jar"
  FileUtils.cp "jetty_config/log4j-1.2.16.jar", "sandbox/webserver/lib/log4j-1.2.16.jar"
  FileUtils.cp "jetty_config/slf4j-api-1.6.1.jar", "sandbox/webserver/lib/slf4j-api-1.6.1.jar"
  FileUtils.cp "jetty_config/log4j.properties", "sandbox/webserver/resources/log4j.properties"
  FileUtils.cp "jetty_config/server.bat", "sandbox"
  FileUtils.cp "jetty_config/server.sh", "sandbox"
  FileUtils.cp "jetty_config/fresh_server.bat", "sandbox"
  FileUtils.cp "jetty_config/fresh_server.sh", "sandbox"
end

desc "inject QEDServer into Jetty"
task :install_qed do
  FileUtils.cp "jetty_config/webserver.xml", "sandbox/webserver/contexts/"
  FileUtils.cp "webserver.war", "sandbox/webserver/webapps/webserver.war"
end

desc "Package QEDServer as a zipfile"
task :package_jetty => [:war, :config_jetty, :install_qed] do
 
  FileUtils.cp "END_USER_README.md", "sandbox/README.txt"
  %w{LICENSE HISTORY.txt}.each do |f|
    FileUtils.cp f, "sandbox/#{f}"
  end
  
  Dir.chdir "sandbox" do
    sh "zip -9 -r #{QEDZIPFILE} *"
  end
  FileUtils.mv "sandbox/#{QEDZIPFILE}", QEDZIPFILE
end

task :war do
  sh "jruby -S warble compiled gemjar war"
end

desc "create the war file" 
task :war_exec do
  FileUtils.rm_rf "public"
  sh "jruby -S warble compiled gemjar executable war"
end

namespace :release do 
  
  desc "update the web site"
  task :website do
    `scp html/* #{SERVER}`
  end

  desc "release new version"
  task :qed => :package do
    `scp #{QEDZIPFILE} #{SERVER}/#{QEDZIPFILE}`
  end

end