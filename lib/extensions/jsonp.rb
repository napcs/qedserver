module JSONP
  def self.included(base)
    base.class_eval do
      alias orig_to_json to_json
      def to_json(*a)
        callback = nil
        if a[0] && !a[0][:callback].blank?
          callback = a[0].delete(:callback)
        end
        r = self.orig_to_json(a[0])
        if callback
          r = "#{callback}(#{r})"
        end
        r
      end
    end
  end
end


Hash.send :include, JSONP
Array.send :include, JSONP
ActiveRecord::Base.send :include, JSONP
ActiveRecord::Relation.send :include, JSONP
