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


task :package_jetty  do
  FileUtils.rm_rf "sandbox"
  FileUtils.mkdir "sandbox"
  FileUtils.cp_r "jetty", "sandbox/webserver"
  FileUtils.rm_rf "sandbox/webserver/contexts/"
  FileUtils.rm_rf "sandbox/webserver/webapps/"
  FileUtils.mkdir "sandbox/webserver/contexts"
  FileUtils.mkdir "sandbox/webserver/webapps"
  FileUtils.cp "jetty_config/webserver.xml", "sandbox/webserver/contexts/"
  FileUtils.cp "webserver.war", "sandbox/webserver/webapps/webserver.war"
  FileUtils.cp "jetty_config/server.bat", "sandbox"
  FileUtils.cp "jetty_config/server.sh", "sandbox"
  FileUtils.cp "jetty_config/fresh_server.bat", "sandbox"
  FileUtils.cp "jetty_config/fresh_server.sh", "sandbox"
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