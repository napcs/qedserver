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
  FileUtils.mkdir "sandbox/qedserver"
  FileUtils.mkdir "sandbox/qedserver/etc"
  FileUtils.mkdir "sandbox/qedserver/resources"
  FileUtils.cp "jetty/start.jar", "sandbox/qedserver/start.jar"
  FileUtils.cp_r "jetty/lib", "sandbox/qedserver/lib"
  FileUtils.cp_r "jetty/LICENSES", "sandbox/qedserver/LICENSES"
  FileUtils.cp "jetty/etc/jetty.xml", "sandbox/qedserver/etc/jetty.xml"
  FileUtils.cp "jetty/etc/webdefault.xml", "sandbox/qedserver/etc/webdefault.xml"
  FileUtils.cp "jetty/etc/realm.properties", "sandbox/qedserver/etc/realm.properties"
  FileUtils.mkdir_p "sandbox/qedserver/logs"
end

desc "inject QEDServer into Jetty"
task :install_qed do
  puts "Configuring folders for QEDServer"
  puts "patching logger"
  FileUtils.cp "jetty_config/slf4j-log4j12-1.6.1.jar", "sandbox/qedserver/lib/slf4j-log4j12-1.6.1.jar"
  FileUtils.cp "jetty_config/log4j-1.2.16.jar", "sandbox/qedserver/lib/log4j-1.2.16.jar"
  FileUtils.cp "jetty_config/slf4j-api-1.6.1.jar", "sandbox/qedserver/lib/slf4j-api-1.6.1.jar"
  FileUtils.cp "jetty_config/log4j.properties", "sandbox/qedserver/resources/log4j.properties"
  puts "updating startup scripts"
  FileUtils.cp_r "jetty_config/server.bat", "sandbox"
  FileUtils.cp_r "jetty_config/fresh_server.bat", "sandbox"
  FileUtils.cp_r "jetty_config/server.command", "sandbox"
  FileUtils.cp_r "jetty_config/fresh_server.command", "sandbox"
  FileUtils.cp_r "jetty_config/server.sh", "sandbox"
  FileUtils.cp_r "jetty_config/fresh_server.sh", "sandbox"
  puts "Copying QEDServer..."
  FileUtils.rm_rf "sandbox/qedserver/contexts/"
  FileUtils.rm_rf "sandbox/qedserver/webapps/"
  FileUtils.mkdir "sandbox/qedserver/contexts"
  FileUtils.mkdir "sandbox/qedserver/webapps"
  FileUtils.cp "jetty_config/qedserver.xml", "sandbox/qedserver/contexts/"
  FileUtils.cp "qedserver.war", "sandbox/qedserver/webapps/qedserver.war"
  puts "QEDServer copied into the sandbox/qedserver folder"
end

desc "Package QEDServer as a zipfile using Jetty"
task :build_release => [:war, :build_jetty, :install_qed, :package] do
  puts "Done"
end

desc "Zips the sandbox, embeds readme files"
task :package do
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


desc "Create the QEDServer war file for use with any servlet container"
task :war do
  sh "jruby -S warble compiled gemjar war"
end


namespace :release do

  desc "update the web site"
  task :website do
    `scp html/* #{SERVER}`
  end

  desc "release new version"
  task :qed => :build_release do
    `scp #{QEDZIPFILE} #{SERVER}/#{QEDZIPFILE}`
  end

end

desc "Package QEDServer as a zipfile using Warbler and Winstone"
task :package_winstone => [:war_winstone, :install_winstone_to_sandbox, :package] do
  puts "Done"
end

desc "create the war file with the Winstone servlet container"
task :war_winstone do
  sh "jruby -S warble compiled gemjar executable war"
end

task :install_winstone_to_sandbox do
  FileUtils.rm_rf "sandbox"
  FileUtils.mkdir "sandbox"
  FileUtils.rm_rf "public"
  FileUtils.cp_r "warbler_config/server.bat", "sandbox"
  FileUtils.cp_r "warbler_config/fresh_server.bat", "sandbox"
  FileUtils.cp_r "warbler_config/server.command", "sandbox"
  FileUtils.cp_r "warbler_config/fresh_server.command", "sandbox"
  FileUtils.cp_r "warbler_config/server.sh", "sandbox"
  FileUtils.cp_r "warbler_config/fresh_server.sh", "sandbox"
  FileUtils.cp "qedserver.war", "sandbox/qedserver.war"

end

